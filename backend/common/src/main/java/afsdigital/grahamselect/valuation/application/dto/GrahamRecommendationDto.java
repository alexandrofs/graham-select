package afsdigital.grahamselect.valuation.application.dto;

import java.math.BigDecimal;

public record GrahamRecommendationDto(
    String ticker,
    BigDecimal currentPrice,
    BigDecimal intrinsicValue,
    BigDecimal marginOfSafety,
    BigDecimal currentAllocationPct,
    BigDecimal targetAllocationPct,
    BigDecimal allocationGap,
    BigDecimal recommendationScore
) {}
