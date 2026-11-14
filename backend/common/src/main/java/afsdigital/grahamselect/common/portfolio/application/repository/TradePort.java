package afsdigital.grahamselect.common.portfolio.application.repository;

import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradePort {
    Trade save(Trade trade);
    
    default boolean exists(
            String userId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker,
            String side
    ) {
        return exists(userId, null, ticker, tradeDate, quantity, price, broker, side);
    }

    boolean exists(
            String userId,
            String profileId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker,
            String side
    );

    List<Trade> findAllByUserId(String userId);
}
