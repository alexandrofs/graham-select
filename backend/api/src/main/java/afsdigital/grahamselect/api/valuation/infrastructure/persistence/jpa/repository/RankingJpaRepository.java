package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.RankedCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingJpaRepository extends JpaRepository<RankedCompanyEntity, String> {

    @Query(value = "SELECT c.ticker as symbol, c.name, iv.intrinsic_value as intrinsic_value, sp.price as current_price, "
            + "CASE WHEN sp.price IS NULL OR sp.price = 0 THEN 0 ELSE (iv.intrinsic_value / sp.price) - 1 END as margin_of_safety, "
            + "iv.calculation_date as intrinsic_value_updated_at, iv.eps as eps, iv.bvps as bvps "
            + "FROM company_intrinsic_value iv "
            + "JOIN (SELECT company_id, MAX(calculation_date) as latest_date FROM company_intrinsic_value GROUP BY company_id) latest_iv "
            + "ON iv.company_id = latest_iv.company_id AND iv.calculation_date = latest_iv.latest_date "
            + "JOIN company c ON c.id = iv.company_id "
            + "JOIN (SELECT company_id, MAX(price_date) as latest_price_date FROM stock_price GROUP BY company_id) lp "
            + "ON iv.company_id = lp.company_id "
            + "JOIN (SELECT company_id, price_date, MAX(price) as price FROM stock_price GROUP BY company_id, price_date) sp "
            + "ON sp.company_id = lp.company_id AND sp.price_date = lp.latest_price_date "
            + "WHERE sp.price IS NOT NULL AND sp.price > 0 "
            + "ORDER BY margin_of_safety DESC "
            + "LIMIT 20", nativeQuery = true)
    List<RankedCompanyEntity> findTop20BestRanked();
}

