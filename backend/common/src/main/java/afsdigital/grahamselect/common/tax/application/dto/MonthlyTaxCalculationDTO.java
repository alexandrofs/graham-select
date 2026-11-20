package afsdigital.grahamselect.common.tax.application.dto;

import afsdigital.grahamselect.common.tax.domain.entities.DarfStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyTaxCalculationDTO(
        int month,
        int year,
        BigDecimal totalEquitySales,
        boolean isEquityExempt,
        BigDecimal exemptProfit,
        BigDecimal commonTaxableProfit,
        BigDecimal commonLossCompensated,
        BigDecimal commonLossCarryforward,
        BigDecimal commonTaxDue,
        BigDecimal fiiTaxableProfit,
        BigDecimal fiiLossCompensated,
        BigDecimal fiiLossCarryforward,
        BigDecimal fiiTaxDue,
        BigDecimal totalTaxDue,
        BigDecimal accumulatedTaxUnder10,
        DarfStatus darfStatus,
        BigDecimal darfAmount,
        LocalDate darfDueDate,
        List<MonthlyTradeOperationDTO> operations
) {
}
