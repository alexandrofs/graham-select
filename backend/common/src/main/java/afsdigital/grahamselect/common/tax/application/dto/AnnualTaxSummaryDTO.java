package afsdigital.grahamselect.common.tax.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnnualTaxSummaryDTO(
        int year,
        List<MonthlyTaxCalculationDTO> monthlyCalculations,
        BigDecimal totalTaxDueYear,
        BigDecimal totalSalesYear,
        BigDecimal totalExemptProfitYear,
        BigDecimal totalTaxableProfitYear,
        BigDecimal finalCommonLossCarryforward,
        BigDecimal finalFiiLossCarryforward
) {
}
