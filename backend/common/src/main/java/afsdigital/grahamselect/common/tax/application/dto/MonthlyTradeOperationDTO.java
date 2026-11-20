package afsdigital.grahamselect.common.tax.application.dto;

import afsdigital.grahamselect.common.tax.domain.entities.TaxCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyTradeOperationDTO(
        String ticker,
        LocalDate tradeDate,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal averagePrice,
        BigDecimal totalSaleValue,
        BigDecimal netProfitOrLoss,
        TaxCategory category
) {
}
