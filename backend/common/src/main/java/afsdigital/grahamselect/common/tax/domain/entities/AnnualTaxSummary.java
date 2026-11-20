package afsdigital.grahamselect.common.tax.domain.entities;

import java.math.BigDecimal;
import java.util.List;

public record AnnualTaxSummary(
        int year,
        List<MonthlyTaxCalculation> monthlyCalculations,
        BigDecimal totalTaxDueYear,
        BigDecimal totalSalesYear,
        BigDecimal totalExemptProfitYear,
        BigDecimal totalTaxableProfitYear,
        BigDecimal finalCommonLossCarryforward,
        BigDecimal finalFiiLossCarryforward
) {
}
