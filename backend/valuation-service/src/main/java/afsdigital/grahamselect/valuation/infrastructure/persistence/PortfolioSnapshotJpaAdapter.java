package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.PortfolioSnapshotPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PortfolioSnapshotJpaAdapter implements PortfolioSnapshotPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> getCurrentAllocationByUserId(String userId) {
        log.info("Calculating portfolio snapshot for user {}", userId);

        String tradeQuery = "SELECT t.ticker, " +
                "SUM(CASE WHEN t.side = 'COMPRA' THEN t.quantity WHEN t.side = 'VENDA' THEN -t.quantity ELSE 0 END) " +
                "FROM trades t " +
                "WHERE t.user_id = :userId " +
                "GROUP BY t.ticker";

        List<Object[]> tradeResults = entityManager.createNativeQuery(tradeQuery)
                .setParameter("userId", userId)
                .getResultList();

        Map<String, BigDecimal> activeQuantities = new HashMap<>();
        for (Object[] row : tradeResults) {
            String ticker = (String) row[0];
            BigDecimal quantity = toBigDecimal(row[1]);
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                activeQuantities.put(ticker, quantity);
            }
        }

        if (activeQuantities.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> tickerPrices = new HashMap<>();
        String priceQuery = "SELECT c.ticker, sp.price " +
                "FROM stock_price sp " +
                "JOIN company c ON c.id = sp.company_id " +
                "JOIN (SELECT company_id, MAX(price_date) as max_date FROM stock_price GROUP BY company_id) latest " +
                "ON latest.company_id = sp.company_id AND latest.max_date = sp.price_date " +
                "WHERE c.ticker IN (:tickers)";

        List<Object[]> priceResults = entityManager.createNativeQuery(priceQuery)
                .setParameter("tickers", activeQuantities.keySet())
                .getResultList();

        for (Object[] row : priceResults) {
            String ticker = (String) row[0];
            BigDecimal price = toBigDecimal(row[1]);
            tickerPrices.put(ticker, price);
        }

        for (String ticker : activeQuantities.keySet()) {
            if (!tickerPrices.containsKey(ticker)) {
                log.warn("No stock price found for ticker: {}. Using zero price.", ticker);
                tickerPrices.put(ticker, BigDecimal.ZERO);
            }
        }

        Map<String, BigDecimal> marketValues = new HashMap<>();
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : activeQuantities.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal qty = entry.getValue();
            BigDecimal price = tickerPrices.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal marketValue = qty.multiply(price);

            marketValues.put(ticker, marketValue);
            totalPortfolioValue = totalPortfolioValue.add(marketValue);
        }

        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> allocation = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : marketValues.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal marketValue = entry.getValue();
            BigDecimal pct = marketValue.multiply(new BigDecimal("100"))
                    .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP);
            allocation.put(ticker, pct);
        }

        return allocation;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
