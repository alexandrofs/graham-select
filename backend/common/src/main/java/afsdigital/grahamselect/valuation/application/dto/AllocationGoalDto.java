package afsdigital.grahamselect.valuation.application.dto;

import java.math.BigDecimal;

public record AllocationGoalDto(
    String goalType, // "ASSET_CLASS" | "TICKER"
    String targetKey,
    BigDecimal targetPercentage
) {}
