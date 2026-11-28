package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain service managing Kerno AI diagnostic cache lookups and TTL-based persistence.
 */
public class AiDiagnosticCacheService {

    private final AiDiagnosticCachePort cachePort;
    private final AiFinOpsProperties properties;
    private final Clock clock;

    public AiDiagnosticCacheService(AiDiagnosticCachePort cachePort, AiFinOpsProperties properties) {
        this(cachePort, properties, Clock.systemUTC());
    }

    public AiDiagnosticCacheService(AiDiagnosticCachePort cachePort, AiFinOpsProperties properties, Clock clock) {
        this.cachePort = Objects.requireNonNull(cachePort, "cachePort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Finds a valid (non-expired) cached diagnostic without debiting any FinOps quota.
     */
    public Optional<AiDiagnosticCacheEntry> findValidDiagnostic(
            Long userId,
            String portfolioStateHash,
            String modelVersion,
            String promptVersion
    ) {
        if (userId == null || isBlank(portfolioStateHash) || isBlank(modelVersion) || isBlank(promptVersion)) {
            return Optional.empty();
        }
        LocalDateTime nowUtc = LocalDateTime.now(clock);
        return cachePort.findValidCache(userId, portfolioStateHash, modelVersion, promptVersion, nowUtc)
                .filter(entry -> !entry.isExpired(nowUtc));
    }

    public Optional<AiDiagnosticCacheEntry> findLatestDiagnostic(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return cachePort.findLatestByUserId(userId);
    }

    public AiDiagnosticCacheEntry saveDiagnostic(
            Long userId,
            String portfolioStateHash,
            String modelVersion,
            String promptVersion,
            String jsonPayload
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        requireNonBlank(portfolioStateHash, "portfolioStateHash");
        requireNonBlank(modelVersion, "modelVersion");
        requireNonBlank(promptVersion, "promptVersion");
        requireNonBlank(jsonPayload, "jsonPayload");

        LocalDateTime nowUtc = LocalDateTime.now(clock);
        LocalDateTime expiresAt = nowUtc.plusHours(properties.cacheTtlHours());

        AiDiagnosticCacheEntry entry = new AiDiagnosticCacheEntry(
                null,
                userId,
                portfolioStateHash,
                modelVersion,
                promptVersion,
                jsonPayload,
                nowUtc,
                expiresAt
        );
        return cachePort.save(entry);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
