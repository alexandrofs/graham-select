package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.application.port.AiUsageCounterPort;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiFreeTierNotAllowedException;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiRateLimitExceededException;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;
import afsdigital.grahamselect.common.ai.domain.model.AiUsageStatus;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain service enforcing Kerno AI FinOps daily and monthly rate limits in UTC.
 */
public class AiFinOpsRateLimitService {

    private final AiUsageCounterPort usageCounterPort;
    private final AiDiagnosticCachePort cachePort;
    private final AiFinOpsProperties properties;
    private final Clock clock;

    public AiFinOpsRateLimitService(
            AiUsageCounterPort usageCounterPort,
            AiDiagnosticCachePort cachePort,
            AiFinOpsProperties properties
    ) {
        this(usageCounterPort, cachePort, properties, Clock.systemUTC());
    }

    public AiFinOpsRateLimitService(
            AiUsageCounterPort usageCounterPort,
            AiDiagnosticCachePort cachePort,
            AiFinOpsProperties properties,
            Clock clock
    ) {
        this.usageCounterPort = Objects.requireNonNull(usageCounterPort, "usageCounterPort must not be null");
        this.cachePort = Objects.requireNonNull(cachePort, "cachePort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Atomically consumes one LLM call quota for Pro/Founder users, or throws if FREE or rate-limited.
     */
    public AiUsageStatus consumeQuotaOrThrow(Long userId, CommercialPlan commercialPlan) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(commercialPlan, "commercialPlan must not be null");

        if (commercialPlan == CommercialPlan.FREE) {
            throw new AiFreeTierNotAllowedException(
                    "Plano FREE não possui cota para chamadas LLM; utilize o fallback determinístico local."
            );
        }

        LocalDateTime nowUtc = LocalDateTime.now(clock);
        LocalDate todayUtc = nowUtc.toLocalDate();

        boolean consumed = usageCounterPort.tryConsumeCall(
                userId,
                todayUtc,
                properties.dailyRateLimit(),
                properties.monthlyRateLimit(),
                nowUtc
        );

        AiUsageStatus status = getUsageStatus(userId, todayUtc);
        if (!consumed) {
            AiDiagnosticCacheEntry latestDiagnostic = cachePort.findLatestByUserId(userId).orElse(null);
            throw new AiRateLimitExceededException(
                    String.format(
                            "Limite de análises de IA atingido (%d/%d diário, %d/%d mensal).",
                            status.dailyCallsUsed(),
                            status.dailyLimit(),
                            status.monthlyCallsUsed(),
                            status.monthlyLimit()
                    ),
                    latestDiagnostic,
                    status.dailyCallsUsed(),
                    status.dailyLimit(),
                    status.monthlyCallsUsed(),
                    status.monthlyLimit()
            );
        }

        return status;
    }

    public AiUsageStatus getUsageStatus(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return getUsageStatus(userId, LocalDate.now(clock));
    }

    private AiUsageStatus getUsageStatus(Long userId, LocalDate todayUtc) {
        LocalDate monthStartUtc = todayUtc.withDayOfMonth(1);
        int dailyUsed = usageCounterPort.getDailyCallCount(userId, todayUtc);
        int monthlyUsed = usageCounterPort.getMonthlyCallCount(userId, monthStartUtc, todayUtc);
        return new AiUsageStatus(
                dailyUsed,
                properties.dailyRateLimit(),
                monthlyUsed,
                properties.monthlyRateLimit(),
                todayUtc
        );
    }
}
