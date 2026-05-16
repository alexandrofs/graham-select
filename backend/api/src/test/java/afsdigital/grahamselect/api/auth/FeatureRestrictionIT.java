package afsdigital.grahamselect.api.auth;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FeatureRestrictionIT extends BaseRepositoryIT {

    private static final String RANKED_COMPANIES_ENDPOINT = "/api/v1/ranked-companies";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
        userRepository.deleteAll();

        String companyId = UUID.randomUUID().toString();
        companyJpaRepository.save(CompanyEntity.builder()
                .id(companyId)
                .ticker("AAPL")
                .name("Apple Inc.")
                .build());

        stockPriceJpaRepository.save(StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(companyId)
                .priceDate(LocalDate.now())
                .price(BigDecimal.valueOf(150.0))
                .build());

        intrinsicValueJpaRepository.save(IntrinsicValueEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(companyId)
                .calculationDate(LocalDate.now())
                .intrinsicValue(BigDecimal.valueOf(200.0))
                .build());
    }

    @Test
    @DisplayName("Should deny ranked companies to FREE users with RFC 7807 response")
    void shouldDenyFreeUsers() throws Exception {
        User user = saveUser("free-sub", SubscriptionTier.FREE, null);
        mockJwtDecoder("free-token", user);

        mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:upgrade-required"))
                .andExpect(jsonPath("$.title").value("Upgrade Required"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.requiredTier").value("PREMIUM"));
    }

    @Test
    @DisplayName("Should allow ranked companies to users with valid TRIAL")
    void shouldAllowValidTrialUsers() throws Exception {
        User user = saveUser("trial-sub", SubscriptionTier.TRIAL, LocalDateTime.now().plusDays(5));
        mockJwtDecoder("trial-token", user);

        mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                        .header("Authorization", "Bearer trial-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    @DisplayName("Should deny expired trial users and lazily downgrade them to FREE")
    void shouldDenyExpiredTrialUsersAndDowngradeThem() throws Exception {
        User user = saveUser("expired-trial-sub", SubscriptionTier.TRIAL, LocalDateTime.now().minusDays(1));
        mockJwtDecoder("expired-trial-token", user);

        mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                        .header("Authorization", "Bearer expired-trial-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:upgrade-required"))
                .andExpect(jsonPath("$.title").value("Upgrade Required"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedUser.getTier()).isEqualTo(SubscriptionTier.FREE);
    }

    @Test
    @DisplayName("Should allow ranked companies to PREMIUM users")
    void shouldAllowPremiumUsers() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, LocalDateTime.now().minusDays(1));
        mockJwtDecoder("premium-token", user);

        mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].marginOfSafety").value(0.3333333333333333));
    }

    private User saveUser(String googleSub, SubscriptionTier tier, LocalDateTime trialEndsAt) {
        return userRepository.save(User.builder()
                .googleSub(googleSub)
                .email(googleSub + "@example.com")
                .fullName("Test User " + googleSub)
                .tier(tier)
                .trialEndsAt(trialEndsAt)
                .build());
    }

    private void mockJwtDecoder(String tokenValue, User user) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .claim("sub", user.getGoogleSub())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);
    }
}
