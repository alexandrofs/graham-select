package afsdigital.grahamselect.api.founders;

import afsdigital.grahamselect.api.auth.infrastructure.security.OAuth2UserProvisioningConverter;
import afsdigital.grahamselect.api.founders.infrastructure.persistence.JdbcFounderQuotaRepositoryAdapter;
import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.entities.FounderQuotaEntity;
import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.repository.FounderQuotaJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class FounderQuotaConcurrencyIT extends BaseRepositoryIT {

    @Autowired
    private FounderQuotaJpaRepository founderQuotaJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2UserProvisioningConverter converter;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        setQuota(250, 250);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        setQuota(250, 250);
    }

    @Test
    @DisplayName("GET /api/v1/founders/slots should be public and reflect active and exhausted states")
    void shouldExposePublicFounderSlotsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/founders/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(250))
                .andExpect(jsonPath("$.remainingSlots").value(250))
                .andExpect(jsonPath("$.isLoteActive").value(true));

        setQuota(250, 0);

        mockMvc.perform(get("/api/v1/founders/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(250))
                .andExpect(jsonPath("$.remainingSlots").value(0))
                .andExpect(jsonPath("$.isLoteActive").value(false));
    }

    @Test
    @DisplayName("Concurrent signups competing for K < N slots must grant exactly K FOUNDER_LIFETIME and N-K PRO_TRIAL")
    void shouldAllocateExactFounderSlotsUnderConcurrency() throws Exception {
        int totalThreads = 12;
        int availableSlots = 5;
        setQuota(250, availableSlots);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        try {
            CountDownLatch readyLatch = new CountDownLatch(totalThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < totalThreads; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    Jwt jwt = buildJwt("concurrent-sub-" + index, "user" + index + "@kerno.test", "User " + index);
                    converter.convert(jwt);
                    return null;
                }));
            }

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(totalThreads);

        long foundersCount = allUsers.stream()
                .filter(u -> u.getCommercialPlan() == CommercialPlan.FOUNDER_LIFETIME)
                .count();
        long proTrialCount = allUsers.stream()
                .filter(u -> u.getCommercialPlan() == CommercialPlan.PRO_TRIAL)
                .count();

        assertThat(foundersCount).isEqualTo(availableSlots);
        assertThat(proTrialCount).isEqualTo(totalThreads - availableSlots);

        allUsers.stream()
                .filter(u -> u.getCommercialPlan() == CommercialPlan.FOUNDER_LIFETIME)
                .forEach(u -> {
                    assertThat(u.getTier()).isEqualTo(SubscriptionTier.PREMIUM);
                    assertThat(u.getTrialExpiresAt()).isNull();
                    assertThat(u.getTrialEndsAt()).isNull();
                });

        LocalDateTime expectedMinExpiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(89);
        allUsers.stream()
                .filter(u -> u.getCommercialPlan() == CommercialPlan.PRO_TRIAL)
                .forEach(u -> {
                    assertThat(u.getTier()).isEqualTo(SubscriptionTier.TRIAL);
                    assertThat(u.getTrialExpiresAt()).isAfter(expectedMinExpiry);
                    assertThat(u.getTrialEndsAt()).isEqualTo(u.getTrialExpiresAt());
                });

        FounderQuotaEntity quota = founderQuotaJpaRepository
                .findById(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID)
                .orElseThrow();
        assertThat(quota.getFounderSlotsRemaining()).isZero();
    }

    @Test
    @DisplayName("Returning user login and JWT without email must not decrement founder quota")
    void shouldNotLeakQuotaOnReturningLoginOrMissingEmail() {
        setQuota(250, 10);

        Jwt validJwt = buildJwt("returning-sub", "returning@kerno.test", "Returning User");
        converter.convert(validJwt);
        converter.convert(validJwt);

        Jwt missingEmailJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("no-email-sub")
                .claim("name", "No Email")
                .build();

        assertThatThrownBy(() -> converter.convert(missingEmailJwt))
                .isInstanceOf(RuntimeException.class);

        FounderQuotaEntity quota = founderQuotaJpaRepository
                .findById(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID)
                .orElseThrow();
        assertThat(quota.getFounderSlotsRemaining()).isEqualTo(9);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    private void setQuota(int totalSlots, int remainingSlots) {
        FounderQuotaEntity entity = founderQuotaJpaRepository
                .findById(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID)
                .orElse(FounderQuotaEntity.builder()
                        .id(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID)
                        .build());
        entity.setTotalSlots(totalSlots);
        entity.setFounderSlotsRemaining(remainingSlots);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        founderQuotaJpaRepository.saveAndFlush(entity);
    }

    private Jwt buildJwt(String sub, String email, String name) {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(sub)
                .claim("email", email)
                .claim("name", name)
                .build();
    }
}
