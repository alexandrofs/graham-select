package afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.entities.B3ImportStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface B3ImportStatusJpaRepository extends JpaRepository<B3ImportStatusEntity, Long> {

    Optional<B3ImportStatusEntity> findByCorrelationIdAndUserId(String correlationId, String userId);

    Optional<B3ImportStatusEntity> findTopByUserIdOrderByCreatedAtDesc(String userId);
}
