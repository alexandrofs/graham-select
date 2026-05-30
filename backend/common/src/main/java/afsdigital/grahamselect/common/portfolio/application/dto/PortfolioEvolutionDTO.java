package afsdigital.grahamselect.common.portfolio.application.dto;

import java.util.List;

public record PortfolioEvolutionDTO(
        List<MonthlyEvolutionDTO> monthlyData
) {
}
