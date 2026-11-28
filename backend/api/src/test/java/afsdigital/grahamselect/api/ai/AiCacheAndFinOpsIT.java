package afsdigital.grahamselect.api.ai;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.application.port.AiUsageCounterPort;
import afsdigital.grahamselect.common.ai.application.service.AiDiagnosticCacheService;
import afsdigital.grahamselect.common.ai.application.service.AiFinOpsRateLimitService;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiFreeTierNotAllowedException;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiRateLimitExceededException;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCacheAndFinOpsIT extends BaseRepositoryIT {

    @Autowired
    private AiDiagnosticCacheService cacheService;

    @Autowired
    private AiFinOpsRateLimitService rateLimitService;

    @Autowired
    private AiUsageCounterPort usageCounterPort;

    @Autowired
    private AiDiagnosticCachePort cachePort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.findByGoogleSub("ai-it-sub").ifPresent(existing -> {
            jdbcTemplate.update("DELETE FROM tb_ai_diagnostic_cache WHERE user_id = ?", existing.getId());
            jdbcTemplate.update("DELETE FROM tb_ai_usage_counter WHERE user_id = ?", existing.getId());
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", existing.getId());
        });

        testUser = userRepository.save(User.builder()
                .googleSub("ai-it-sub")
                .email("ai-it@kerno.test")
                .fullName("AI IT User")
                .tier(SubscriptionTier.PREMIUM)
                .commercialPlan(CommercialPlan.FOUNDER_LIFETIME)
                .build());
    }

    @Test
    @DisplayName("Cache hit must respond in < 400ms without incrementing tb_ai_usage_counter")
    void shouldReturnCachedDiagnosticFastWithoutDebitingQuota() {
        cacheService.saveDiagnostic(
                testUser.getId(),
                "sha256-portfolio-abc",
                "gemini-2.5-flash",
                "v1",
                "{\"resumoExecutivo\":\"Carteira equilibrada\"}"
        );

        // Warm up JPA/JDBC query path before measuring latency
        assertThat(cacheService.findValidDiagnostic(
                testUser.getId(),
                "sha256-portfolio-abc",
                "gemini-2.5-flash",
                "v1"
        )).isPresent();

        long startNanos = System.nanoTime();
        Optional<AiDiagnosticCacheEntry> hit = cacheService.findValidDiagnostic(
                testUser.getId(),
                "sha256-portfolio-abc",
                "gemini-2.5-flash",
                "v1"
        );
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        assertThat(hit).isPresent();
        assertThat(hit.get().jsonPayload()).contains("Carteira equilibrada");
        assertThat(elapsedMs).isLessThan(400L);

        Integer usageRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_ai_usage_counter WHERE user_id = ?",
                Integer.class,
                testUser.getId()
        );
        assertThat(usageRows).isZero();

        assertThat(cacheService.findValidDiagnostic(
                testUser.getId(),
                "sha256-portfolio-abc",
                "gemini-2.5-flash",
                "v2"
        )).isEmpty();
    }

    @Test
    @DisplayName("Concurrent FinOps quota requests must allow at most 5 calls/day and attach latest cache on 429")
    void shouldEnforceAtomicDailyLimitUnderConcurrency() throws Exception {
        AiDiagnosticCacheEntry cached = cacheService.saveDiagnostic(
                testUser.getId(),
                "sha256-latest",
                "gemini-2.5-flash",
                "v1",
                "{\"resumoExecutivo\":\"Ultimo cache\"}"
        );

        int totalThreads = 12;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);

        try {
            CountDownLatch readyLatch = new CountDownLatch(totalThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < totalThreads; i++) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    try {
                        rateLimitService.consumeQuotaOrThrow(testUser.getId(), CommercialPlan.FOUNDER_LIFETIME);
                        successCount.incrementAndGet();
                    } catch (AiRateLimitExceededException ex) {
                        assertThat(ex.getLatestDiagnostic()).isNotNull();
                        assertThat(ex.getLatestDiagnostic().id()).isEqualTo(cached.id());
                        rateLimitedCount.incrementAndGet();
                    }
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

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(rateLimitedCount.get()).isEqualTo(7);
        assertThat(usageCounterPort.getDailyCallCount(testUser.getId(), LocalDate.now(ZoneOffset.UTC)))
                .isEqualTo(5);
    }

    @Test
    @DisplayName("Monthly aggregation SUM(daily_call_count) must block when 30 calls reached in current UTC month")
    void shouldEnforceMonthlyLimitAcrossMultipleDaysInCurrentUtcMonth() {
        LocalDate midMonthUtc = LocalDate.of(2026, 11, 15);
        LocalDate monthStartUtc = LocalDate.of(2026, 11, 1);
        LocalDateTime midMonthNowUtc = LocalDateTime.of(2026, 11, 15, 12, 0);

        jdbcTemplate.update(
                "INSERT INTO tb_ai_usage_counter (user_id, usage_date, daily_call_count, updated_at) VALUES (?, ?, 30, ?)",
                testUser.getId(), LocalDate.of(2026, 11, 10), midMonthNowUtc
        );

        assertThat(usageCounterPort.getMonthlyCallCount(testUser.getId(), monthStartUtc, midMonthUtc))
                .isEqualTo(30);
        assertThat(usageCounterPort.tryConsumeCall(testUser.getId(), midMonthUtc, 5, 30, midMonthNowUtc))
                .isFalse();

        AiFinOpsRateLimitService fixedMidMonthService = new AiFinOpsRateLimitService(
                usageCounterPort,
                cachePort,
                new AiFinOpsProperties(5, 30, 24),
                Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> fixedMidMonthService.consumeQuotaOrThrow(testUser.getId(), CommercialPlan.PRO_TRIAL))
                .isInstanceOf(AiRateLimitExceededException.class)
                .satisfies(ex -> {
                    AiRateLimitExceededException rateEx = (AiRateLimitExceededException) ex;
                    assertThat(rateEx.getDailyCallsUsed()).isZero();
                    assertThat(rateEx.getMonthlyCallsUsed()).isEqualTo(30);
                });
    }

    @Test
    @DisplayName("deleteByUserId must remove records on both cache and usage counter ports")
    void shouldDeleteAllRecordsByUserIdOnBothPorts() {
        cacheService.saveDiagnostic(
                testUser.getId(),
                "sha256-delete-test",
                "gemini-2.5-flash",
                "v1",
                "{\"resumoExecutivo\":\"Para deletar\"}"
        );
        rateLimitService.consumeQuotaOrThrow(testUser.getId(), CommercialPlan.FOUNDER_LIFETIME);

        cachePort.deleteByUserId(testUser.getId());
        usageCounterPort.deleteByUserId(testUser.getId());

        assertThat(cachePort.findLatestByUserId(testUser.getId())).isEmpty();
        assertThat(usageCounterPort.getDailyCallCount(testUser.getId(), LocalDate.now(ZoneOffset.UTC))).isZero();
    }

    @Test
    @DisplayName("FREE tier must throw AiFreeTierNotAllowedException without touching tb_ai_usage_counter")
    void shouldRejectFreeTierWithoutPersistingUsage() {
        assertThatThrownBy(() -> rateLimitService.consumeQuotaOrThrow(testUser.getId(), CommercialPlan.FREE))
                .isInstanceOf(AiFreeTierNotAllowedException.class);

        Integer usageRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_ai_usage_counter WHERE user_id = ?",
                Integer.class,
                testUser.getId()
        );
        assertThat(usageRows).isZero();
    }
}
