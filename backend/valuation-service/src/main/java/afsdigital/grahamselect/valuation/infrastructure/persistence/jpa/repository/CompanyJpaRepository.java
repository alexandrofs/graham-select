package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, UUID> {

    Optional<CompanyEntity> findByTicker(String ticker);

}
