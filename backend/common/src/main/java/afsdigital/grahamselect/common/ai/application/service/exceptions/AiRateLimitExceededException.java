package afsdigital.grahamselect.common.ai.application.service.exceptions;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;

/**
 * Thrown when a Pro/Founder user exceeds the daily or monthly Kerno AI FinOps rate limit.
 */
public class AiRateLimitExceededException extends RuntimeException {

    private final AiDiagnosticCacheEntry latestDiagnostic;
    private final int dailyCallsUsed;
    private final int dailyLimit;
    private final int monthlyCallsUsed;
    private final int monthlyLimit;

    public AiRateLimitExceededException(
            String message,
            AiDiagnosticCacheEntry latestDiagnostic,
            int dailyCallsUsed,
            int dailyLimit,
            int monthlyCallsUsed,
            int monthlyLimit
    ) {
        super(message);
        this.latestDiagnostic = latestDiagnostic;
        this.dailyCallsUsed = dailyCallsUsed;
        this.dailyLimit = dailyLimit;
        this.monthlyCallsUsed = monthlyCallsUsed;
        this.monthlyLimit = monthlyLimit;
    }

    public AiDiagnosticCacheEntry getLatestDiagnostic() {
        return latestDiagnostic;
    }

    public int getDailyCallsUsed() {
        return dailyCallsUsed;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public int getMonthlyCallsUsed() {
        return monthlyCallsUsed;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }
}
