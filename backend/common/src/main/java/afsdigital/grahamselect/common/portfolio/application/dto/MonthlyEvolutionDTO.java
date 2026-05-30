package afsdigital.grahamselect.common.portfolio.application.dto;

import java.math.BigDecimal;

public record MonthlyEvolutionDTO(
        String month,
        BigDecimal totalContributions,
        BigDecimal totalDividends
) {
}
