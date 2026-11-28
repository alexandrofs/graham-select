package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.application.port.AiUsageCounterPort;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiFreeTierNotAllowedException;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiRateLimitExceededException;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;
import afsdigital.grahamselect.common.ai.domain.model.AiUsageStatus;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFinOpsRateLimitServiceTest {

    private static final Instant DAY_END_INSTANT = Instant.parse("2026-11-30T23:59:59Z");
    private static final Instant NEXT_MONTH_INSTANT = Instant.parse("2026-12-01T00:00:01Z");

    @Mock
    private AiUsageCounterPort usageCounterPort;

    @Mock
    private AiDiagnosticCachePort cachePort;

    private AiFinOpsRateLimitService createService(Instant instant) {
        return new AiFinOpsRateLimitService(
                usageCounterPort,
                cachePort,
                new AiFinOpsProperties(5, 30, 24),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldConsumeQuotaForFounderAndProTrialWithinLimits() {
        LocalDate dateUtc = LocalDate.of(2026, 11, 30);
        LocalDateTime nowUtc = LocalDateTime.ofInstant(DAY_END_INSTANT, ZoneOffset.UTC);

        when(usageCounterPort.tryConsumeCall(1L, dateUtc, 5, 30, nowUtc)).thenReturn(true);
        when(usageCounterPort.getDailyCallCount(1L, dateUtc)).thenReturn(1);
        when(usageCounterPort.getMonthlyCallCount(1L, LocalDate.of(2026, 11, 1), dateUtc)).thenReturn(10);

        AiUsageStatus status = createService(DAY_END_INSTANT)
                .consumeQuotaOrThrow(1L, CommercialPlan.FOUNDER_LIFETIME);

        assertThat(status.dailyCallsUsed()).isEqualTo(1);
        assertThat(status.dailyRemaining()).isEqualTo(4);
        assertThat(status.monthlyCallsUsed()).isEqualTo(10);
        assertThat(status.monthlyRemaining()).isEqualTo(20);
        assertThat(status.usageDateUtc()).isEqualTo(dateUtc);
    }

    @Test
    void shouldThrowRateLimitExceededWithLatestDiagnosticWhenDailyLimitReached() {
        LocalDate dateUtc = LocalDate.of(2026, 11, 30);
        LocalDateTime nowUtc = LocalDateTime.ofInstant(DAY_END_INSTANT, ZoneOffset.UTC);
        AiDiagnosticCacheEntry latest = new AiDiagnosticCacheEntry(
                99L, 1L, "hash", "gemini-2.5-flash", "v1", "{\"cached\":true}", nowUtc.minusHours(1), nowUtc.plusHours(23)
        );

        when(usageCounterPort.tryConsumeCall(1L, dateUtc, 5, 30, nowUtc)).thenReturn(false);
        when(usageCounterPort.getDailyCallCount(1L, dateUtc)).thenReturn(5);
        when(usageCounterPort.getMonthlyCallCount(1L, LocalDate.of(2026, 11, 1), dateUtc)).thenReturn(12);
        when(cachePort.findLatestByUserId(1L)).thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> createService(DAY_END_INSTANT).consumeQuotaOrThrow(1L, CommercialPlan.PRO_TRIAL))
                .isInstanceOf(AiRateLimitExceededException.class)
                .satisfies(ex -> {
                    AiRateLimitExceededException rateEx = (AiRateLimitExceededException) ex;
                    assertThat(rateEx.getLatestDiagnostic()).isEqualTo(latest);
                    assertThat(rateEx.getDailyCallsUsed()).isEqualTo(5);
                    assertThat(rateEx.getMonthlyCallsUsed()).isEqualTo(12);
                });
    }

    @Test
    void shouldThrowRateLimitExceededWhenMonthlyLimitReached() {
        LocalDate dateUtc = LocalDate.of(2026, 11, 30);
        LocalDateTime nowUtc = LocalDateTime.ofInstant(DAY_END_INSTANT, ZoneOffset.UTC);

        when(usageCounterPort.tryConsumeCall(1L, dateUtc, 5, 30, nowUtc)).thenReturn(false);
        when(usageCounterPort.getDailyCallCount(1L, dateUtc)).thenReturn(2);
        when(usageCounterPort.getMonthlyCallCount(1L, LocalDate.of(2026, 11, 1), dateUtc)).thenReturn(30);
        when(cachePort.findLatestByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService(DAY_END_INSTANT).consumeQuotaOrThrow(1L, CommercialPlan.FOUNDER_LIFETIME))
                .isInstanceOf(AiRateLimitExceededException.class)
                .satisfies(ex -> {
                    AiRateLimitExceededException rateEx = (AiRateLimitExceededException) ex;
                    assertThat(rateEx.getLatestDiagnostic()).isNull();
                    assertThat(rateEx.getMonthlyCallsUsed()).isEqualTo(30);
                });
    }

    @Test
    void shouldResetDailyAndMonthlyWindowOnUtcRollover() {
        LocalDate decFirst = LocalDate.of(2026, 12, 1);
        LocalDateTime nowUtc = LocalDateTime.ofInstant(NEXT_MONTH_INSTANT, ZoneOffset.UTC);

        when(usageCounterPort.tryConsumeCall(1L, decFirst, 5, 30, nowUtc)).thenReturn(true);
        when(usageCounterPort.getDailyCallCount(1L, decFirst)).thenReturn(1);
        when(usageCounterPort.getMonthlyCallCount(1L, decFirst, decFirst)).thenReturn(1);

        AiUsageStatus status = createService(NEXT_MONTH_INSTANT)
                .consumeQuotaOrThrow(1L, CommercialPlan.PRO_TRIAL);

        assertThat(status.usageDateUtc()).isEqualTo(decFirst);
        assertThat(status.dailyCallsUsed()).isEqualTo(1);
        assertThat(status.monthlyCallsUsed()).isEqualTo(1);
    }

    @Test
    void shouldRejectFreeTierWithoutDebitingQuota() {
        assertThatThrownBy(() -> createService(DAY_END_INSTANT).consumeQuotaOrThrow(1L, CommercialPlan.FREE))
                .isInstanceOf(AiFreeTierNotAllowedException.class);

        verify(usageCounterPort, never()).tryConsumeCall(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
