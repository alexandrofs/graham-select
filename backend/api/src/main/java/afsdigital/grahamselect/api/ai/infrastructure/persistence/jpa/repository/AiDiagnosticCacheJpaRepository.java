package afsdigital.grahamselect.api.ai.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.ai.infrastructure.persistence.jpa.entities.AiDiagnosticCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiDiagnosticCacheJpaRepository extends JpaRepository<AiDiagnosticCacheEntity, Long> {

    Optional<AiDiagnosticCacheEntity> findFirstByUserIdAndPortfolioStateHashAndModelVersionAndPromptVersionAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            String portfolioStateHash,
            String modelVersion,
            String promptVersion,
            LocalDateTime nowUtc
    );

    Optional<AiDiagnosticCacheEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserId(Long userId);
}
