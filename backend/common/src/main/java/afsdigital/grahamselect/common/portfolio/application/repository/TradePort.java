package afsdigital.grahamselect.common.portfolio.application.repository;

import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TradePort {
    Trade save(Trade trade);
    
    boolean exists(
            String userId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker
    );
}
