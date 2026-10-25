package afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngestionAuditRepository extends JpaRepository<IngestionAudit, Long> {
    List<IngestionAudit> findAllByUserIdOrderByUploadDateDesc(String userId);
    Optional<IngestionAudit> findByCorrelationId(String correlationId);
    List<IngestionAudit> findTop20ByOrderByUploadDateDesc();

    @Query("SELECT DISTINCT a.userId FROM IngestionAudit a WHERE a.uploadDate >= :since")
    List<String> findDistinctUserIdsSince(@Param("since") LocalDateTime since);
}
