package afsdigital.grahamselect.api.admin.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminGrahamLatencyMetricsDto(
        BigDecimal averageLatencyMs,
        BigDecimal p95LatencyMs,
        long slaTargetMs,
        boolean withinSla,
        List<AdminLatencyPointDto> latencyHistory
) {
}
