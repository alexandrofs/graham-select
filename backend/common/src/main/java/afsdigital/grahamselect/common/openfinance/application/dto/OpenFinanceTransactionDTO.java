package afsdigital.grahamselect.common.openfinance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenFinanceTransactionDTO(
        String transactionId,
        String ticker,
        LocalDate tradeDate,
        BigDecimal quantity,
        BigDecimal price,
        String side,
        String broker,
        String memo
) {}
