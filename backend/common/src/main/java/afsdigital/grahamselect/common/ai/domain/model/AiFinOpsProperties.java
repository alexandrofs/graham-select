package afsdigital.grahamselect.common.ai.domain.model;

/**
 * FinOps rate limiting and cache TTL settings for Kerno AI.
 */
public record AiFinOpsProperties(
        int dailyRateLimit,
        int monthlyRateLimit,
        int cacheTtlHours
) {
    public AiFinOpsProperties {
        if (dailyRateLimit < 0) {
            throw new IllegalArgumentException("dailyRateLimit must be >= 0");
        }
        if (monthlyRateLimit < 0) {
            throw new IllegalArgumentException("monthlyRateLimit must be >= 0");
        }
        if (cacheTtlHours <= 0) {
            throw new IllegalArgumentException("cacheTtlHours must be > 0");
        }
    }
}
