package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockPriceJpaRepository extends JpaRepository<StockPriceEntity, String> {
    Optional<StockPriceEntity> findFirstByCompanyIdOrderByPriceDateDesc(String companyId);

    @Query("SELECT sp FROM StockPriceEntity sp WHERE sp.companyId IN :companyIds AND sp.priceDate = (SELECT MAX(sp2.priceDate) FROM StockPriceEntity sp2 WHERE sp2.companyId = sp.companyId)")
    List<StockPriceEntity> findLatestByCompanyIds(@Param("companyIds") List<String> companyIds);
}
