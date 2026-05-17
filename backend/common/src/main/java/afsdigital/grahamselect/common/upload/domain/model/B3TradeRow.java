package afsdigital.grahamselect.common.upload.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record B3TradeRow(
        long lineNumber,
        String ticker,
        LocalDate tradeDate,
        BigDecimal quantity,
        BigDecimal price,
        String broker
) {
}
