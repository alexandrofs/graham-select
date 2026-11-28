package afsdigital.grahamselect.common.ai.application.port;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Outbound port for atomic FinOps AI usage tracking per user and UTC date.
 */
public interface AiUsageCounterPort {

    /**
     * Atomically increments the user's daily call count if both daily and monthly UTC limits allow it.
     *
     * @return {@code true} if the call was consumed, {@code false} if daily or monthly limit was reached.
     */
    boolean tryConsumeCall(
            Long userId,
            LocalDate usageDateUtc,
            int dailyLimit,
            int monthlyLimit,
            LocalDateTime nowUtc
    );

    int getDailyCallCount(Long userId, LocalDate usageDateUtc);

    int getMonthlyCallCount(Long userId, LocalDate monthStartUtc, LocalDate usageDateUtc);

    void deleteByUserId(Long userId);
}
