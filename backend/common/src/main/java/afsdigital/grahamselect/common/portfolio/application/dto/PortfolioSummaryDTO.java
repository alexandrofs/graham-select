package afsdigital.grahamselect.common.portfolio.application.dto;

import java.math.BigDecimal;

public record PortfolioSummaryDTO(
        BigDecimal totalEquity,
        BigDecimal grossYieldPercentage,
        BigDecimal accumulatedDividends,
        BigDecimal monthlyProjection
) {
}
