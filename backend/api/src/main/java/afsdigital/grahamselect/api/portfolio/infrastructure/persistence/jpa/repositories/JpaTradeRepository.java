package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface JpaTradeRepository extends JpaRepository<TradeEntity, UUID> {
    boolean existsByUserIdAndTickerAndTradeDateAndQuantityAndPriceAndBrokerAndSide(
            String userId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker,
            String side
    );
}
