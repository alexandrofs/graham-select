package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface StockPriceJpaRepository extends JpaRepository<StockPriceEntity, String> {
    Optional<StockPriceEntity> findFirstByCompanyIdAndPriceDate(String companyId, LocalDate priceDate);
}

