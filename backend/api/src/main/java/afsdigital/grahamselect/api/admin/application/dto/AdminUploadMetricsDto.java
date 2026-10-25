package afsdigital.grahamselect.api.admin.application.dto;

import java.math.BigDecimal;

public record AdminUploadMetricsDto(
        long totalUploads,
        long successCount,
        long partialCount,
        long errorCount,
        long processingCount,
        BigDecimal errorRatePercentage
) {
}
