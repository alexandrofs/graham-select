package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockPriceJpaRepository extends JpaRepository<StockPriceEntity, String> {
}
