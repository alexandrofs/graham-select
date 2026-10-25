package afsdigital.grahamselect.api.admin.application.dto;

import java.math.BigDecimal;

public record AdminLatencyPointDto(
        String label,
        BigDecimal latencyMs
) {
}
