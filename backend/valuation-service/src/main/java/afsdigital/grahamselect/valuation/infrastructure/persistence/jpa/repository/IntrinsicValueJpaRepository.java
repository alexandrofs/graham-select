package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface IntrinsicValueJpaRepository extends JpaRepository<IntrinsicValueEntity, String> {
    Optional<IntrinsicValueEntity> findByCompanyIdAndCalculationDate(String companyId, LocalDate calculationDate);
}
