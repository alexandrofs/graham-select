package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.ManualTradeAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaManualTradeAuditRepository extends JpaRepository<ManualTradeAuditEntity, UUID> {
}
