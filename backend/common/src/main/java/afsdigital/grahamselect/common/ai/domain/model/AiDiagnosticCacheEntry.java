package afsdigital.grahamselect.common.ai.domain.model;

import java.time.LocalDateTime;

/**
 * Domain representation of a cached Kerno AI portfolio diagnostic entry.
 */
public record AiDiagnosticCacheEntry(
        Long id,
        Long userId,
        String portfolioStateHash,
        String modelVersion,
        String promptVersion,
        String jsonPayload,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
    /**
     * Returns true when the cache entry is expired at {@code nowUtc} ({@code expiresAt <= nowUtc}).
     */
    public boolean isExpired(LocalDateTime nowUtc) {
        return expiresAt == null || !expiresAt.isAfter(nowUtc);
    }
}
