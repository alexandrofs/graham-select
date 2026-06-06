package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    List<TradeEntity> findAllByUserId(String userId);

    @Query("SELECT t.ticker FROM TradeEntity t GROUP BY t.ticker HAVING SUM(CASE WHEN t.side = 'COMPRA' THEN t.quantity ELSE -t.quantity END) > 0")
    List<String> findDistinctTickers();
}
