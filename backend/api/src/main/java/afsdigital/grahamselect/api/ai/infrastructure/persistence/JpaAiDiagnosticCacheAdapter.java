package afsdigital.grahamselect.api.ai.infrastructure.persistence;

import afsdigital.grahamselect.api.ai.infrastructure.persistence.jpa.entities.AiDiagnosticCacheEntity;
import afsdigital.grahamselect.api.ai.infrastructure.persistence.jpa.repository.AiDiagnosticCacheJpaRepository;
import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAiDiagnosticCacheAdapter implements AiDiagnosticCachePort {

    private final AiDiagnosticCacheJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AiDiagnosticCacheEntry> findValidCache(
            Long userId,
            String portfolioStateHash,
            String modelVersion,
            String promptVersion,
            LocalDateTime nowUtc
    ) {
        return jpaRepository
                .findFirstByUserIdAndPortfolioStateHashAndModelVersionAndPromptVersionAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        portfolioStateHash,
                        modelVersion,
                        promptVersion,
                        nowUtc
                )
                .map(JpaAiDiagnosticCacheAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiDiagnosticCacheEntry> findLatestByUserId(Long userId) {
        return jpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(JpaAiDiagnosticCacheAdapter::toDomain);
    }

    @Override
    @Transactional
    public AiDiagnosticCacheEntry save(AiDiagnosticCacheEntry entry) {
        AiDiagnosticCacheEntity entity = AiDiagnosticCacheEntity.builder()
                .id(entry.id())
                .userId(entry.userId())
                .portfolioStateHash(entry.portfolioStateHash())
                .modelVersion(entry.modelVersion())
                .promptVersion(entry.promptVersion())
                .jsonPayload(entry.jsonPayload())
                .createdAt(entry.createdAt())
                .expiresAt(entry.expiresAt())
                .build();
        AiDiagnosticCacheEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    private static AiDiagnosticCacheEntry toDomain(AiDiagnosticCacheEntity entity) {
        return new AiDiagnosticCacheEntry(
                entity.getId(),
                entity.getUserId(),
                entity.getPortfolioStateHash(),
                entity.getModelVersion(),
                entity.getPromptVersion(),
                entity.getJsonPayload(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
