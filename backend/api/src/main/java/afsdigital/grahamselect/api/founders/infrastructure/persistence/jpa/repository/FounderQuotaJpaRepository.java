package afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.entities.FounderQuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FounderQuotaJpaRepository extends JpaRepository<FounderQuotaEntity, Long> {
}
