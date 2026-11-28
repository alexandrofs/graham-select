package afsdigital.grahamselect.common.ai.application.port;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Outbound port for persisting and querying Kerno AI diagnostic cache entries.
 */
public interface AiDiagnosticCachePort {

    Optional<AiDiagnosticCacheEntry> findValidCache(
            Long userId,
            String portfolioStateHash,
            String modelVersion,
            String promptVersion,
            LocalDateTime nowUtc
    );

    Optional<AiDiagnosticCacheEntry> findLatestByUserId(Long userId);

    AiDiagnosticCacheEntry save(AiDiagnosticCacheEntry entry);

    void deleteByUserId(Long userId);
}
