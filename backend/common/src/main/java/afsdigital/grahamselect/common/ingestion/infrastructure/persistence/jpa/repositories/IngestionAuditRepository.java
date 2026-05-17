package afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngestionAuditRepository extends JpaRepository<IngestionAudit, Long> {
    List<IngestionAudit> findAllByUserIdOrderByUploadDateDesc(String userId);
    Optional<IngestionAudit> findByCorrelationId(String correlationId);
}
