package afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities.OpenFinanceConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpenFinanceConnectionJpaRepository extends JpaRepository<OpenFinanceConnectionEntity, UUID> {

    Optional<OpenFinanceConnectionEntity> findByIdAndUserId(UUID id, String userId);

    Optional<OpenFinanceConnectionEntity> findByUserIdAndInstitutionIdAndProfileId(String userId, String institutionId, UUID profileId);

    List<OpenFinanceConnectionEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<OpenFinanceConnectionEntity> findByStatus(String status);

    @Query("SELECT c FROM OpenFinanceConnectionEntity c WHERE c.status = :status AND (c.consentExpiresAt IS NULL OR c.consentExpiresAt > :now)")
    List<OpenFinanceConnectionEntity> findActiveNotExpired(@Param("status") String status, @Param("now") LocalDateTime now);
}
