package afsdigital.grahamselect.valuation.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

public record GrahamRecommendationDto(
    String ticker,
    BigDecimal currentPrice,
    BigDecimal intrinsicValue,
    BigDecimal marginOfSafety,
    BigDecimal currentAllocationPct,
    BigDecimal targetAllocationPct,
    BigDecimal allocationGap,
    BigDecimal recommendationScore,
    @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal epsUsed,
    @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal bvpsUsed
) {}
