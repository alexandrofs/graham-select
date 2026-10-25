package afsdigital.grahamselect.api.admin.application.dto;

public record AdminUserMetricsDto(
        long activeUsers24h,
        long totalUsers,
        long trialUsers,
        long freeUsers,
        long premiumUsers
) {
}
