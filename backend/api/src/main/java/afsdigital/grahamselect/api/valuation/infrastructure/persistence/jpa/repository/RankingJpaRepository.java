package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.RankedCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingJpaRepository extends JpaRepository<RankedCompanyEntity, String> {

    @Query(value = "SELECT c.id as symbol, c.name, iv.intrinsic_value as intrinsic_value, sp.price as current_price, (iv.intrinsic_value / sp.price) - 1 as margin_of_safety "
            +
            "FROM company_intrinsic_value iv " +
            "JOIN (SELECT company_id, MAX(calculation_date) as latest_date FROM company_intrinsic_value GROUP BY company_id) latest_iv "
            +
            "ON iv.company_id = latest_iv.company_id AND iv.calculation_date = latest_iv.latest_date " +
            "JOIN company c ON c.id = iv.company_id " +
            "JOIN (SELECT company_id, MAX(price_date) as latest_price_date FROM stock_price GROUP BY company_id) lp " +
            "ON iv.company_id = lp.company_id " +
            "JOIN stock_price sp ON sp.company_id = lp.company_id AND sp.price_date = lp.latest_price_date " +
            "ORDER BY margin_of_safety DESC " +
            "LIMIT 20", nativeQuery = true)
    List<RankedCompanyEntity> findTop20BestRanked();
}
