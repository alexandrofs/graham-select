package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingJpaRepository extends JpaRepository<IntrinsicValueEntity, String> {

    @Query(value = "SELECT iv.* FROM company_intrinsic_value iv " +
            "JOIN (SELECT company_id, MAX(calculation_date) as latest_date FROM company_intrinsic_value GROUP BY company_id) latest_iv "
            +
            "ON iv.company_id = latest_iv.company_id AND iv.calculation_date = latest_iv.latest_date " +
            "JOIN (SELECT company_id, MAX(price_date) as latest_price_date FROM stock_price GROUP BY company_id) lp " +
            "ON iv.company_id = lp.company_id " +
            "JOIN stock_price sp ON sp.company_id = lp.company_id AND sp.price_date = lp.latest_price_date " +
            "ORDER BY (iv.intrinsic_value / sp.price) DESC " +
            "LIMIT 20", nativeQuery = true)
    List<IntrinsicValueEntity> findTop20BestRanked();
}
