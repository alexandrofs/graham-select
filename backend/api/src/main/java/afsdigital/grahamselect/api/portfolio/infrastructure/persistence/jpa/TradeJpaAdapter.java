package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TradeJpaAdapter implements TradePort {

    private final JpaTradeRepository repository;

    @Override
    public Trade save(Trade trade) {
        TradeEntity entity = TradeEntity.builder()
                .id(trade.getId())
                .userId(trade.getUserId())
                .ticker(trade.getTicker())
                .tradeDate(trade.getTradeDate())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .broker(trade.getBroker())
                .createdAt(trade.getCreatedAt())
                .build();

        TradeEntity saved = repository.save(entity);

        return Trade.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .ticker(saved.getTicker())
                .tradeDate(saved.getTradeDate())
                .quantity(saved.getQuantity())
                .price(saved.getPrice())
                .broker(saved.getBroker())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public boolean exists(
            String userId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker
    ) {
        return repository.existsByUserIdAndTickerAndTradeDateAndQuantityAndPriceAndBroker(
                userId, ticker, tradeDate, quantity, price, broker
        );
    }
}
