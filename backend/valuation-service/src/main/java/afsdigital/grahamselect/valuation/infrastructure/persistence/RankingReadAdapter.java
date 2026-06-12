package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RankingReadAdapter implements RankingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<RankedCompany> findTop20BestRanked() {
        log.info("Fetching top 20 best ranked companies for valuation");

        String sql = "SELECT c.ticker as symbol, c.name, iv.intrinsic_value as intrinsic_value, sp.price as current_price, (iv.intrinsic_value / NULLIF(sp.price, 0)) - 1 as margin_of_safety "
                + "FROM company_intrinsic_value iv "
                + "JOIN (SELECT company_id, MAX(calculation_date) as latest_date FROM company_intrinsic_value GROUP BY company_id) latest_iv "
                + "ON iv.company_id = latest_iv.company_id AND iv.calculation_date = latest_iv.latest_date "
                + "JOIN company c ON c.id = iv.company_id "
                + "JOIN (SELECT company_id, MAX(price_date) as latest_price_date FROM stock_price GROUP BY company_id) lp "
                + "ON iv.company_id = lp.company_id "
                + "JOIN (SELECT company_id, price_date, MAX(price) as price FROM stock_price GROUP BY company_id, price_date) sp "
                + "ON sp.company_id = lp.company_id AND sp.price_date = lp.latest_price_date "
                + "ORDER BY margin_of_safety DESC "
                + "LIMIT 20";

        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        return results.stream()
                .map(row -> {
                    BigDecimal intrinsicVal = toBigDecimal(row[2]);
                    BigDecimal currentPr = toBigDecimal(row[3]);
                    BigDecimal marginSafety = toBigDecimal(row[4]);

                    return RankedCompany.builder()
                            .symbol((String) row[0])
                            .name((String) row[1])
                            .intrinsicValue(intrinsicVal)
                            .currentPrice(currentPr)
                            .marginOfSafety(marginSafety)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
