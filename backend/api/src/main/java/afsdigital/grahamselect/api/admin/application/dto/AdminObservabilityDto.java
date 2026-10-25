package afsdigital.grahamselect.api.admin.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminObservabilityDto(
        AdminUserMetricsDto userMetrics,
        AdminUploadMetricsDto uploadMetrics,
        AdminGrahamLatencyMetricsDto grahamLatencyMetrics,
        List<AnonymizedUploadTraceDto> recentUploads,
        OffsetDateTime generatedAt
) {
}
