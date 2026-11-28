package afsdigital.grahamselect.common.ai.domain.model;

import java.time.LocalDate;

/**
 * Current AI FinOps quota consumption status for a user in UTC.
 */
public record AiUsageStatus(
        int dailyCallsUsed,
        int dailyLimit,
        int monthlyCallsUsed,
        int monthlyLimit,
        LocalDate usageDateUtc
) {
    public int dailyRemaining() {
        return Math.max(0, dailyLimit - dailyCallsUsed);
    }

    public int monthlyRemaining() {
        return Math.max(0, monthlyLimit - monthlyCallsUsed);
    }
}
