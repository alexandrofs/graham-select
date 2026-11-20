package afsdigital.grahamselect.common.tax.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyTradeOperation(
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
