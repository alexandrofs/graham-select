package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiDiagnosticCacheServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-27T15:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private AiDiagnosticCachePort cachePort;

    private AiDiagnosticCacheService createService() {
        return new AiDiagnosticCacheService(cachePort, new AiFinOpsProperties(5, 30, 24), FIXED_CLOCK);
    }

    @Test
    void shouldReturnValidCacheEntryWhenNotExpired() {
        LocalDateTime nowUtc = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        AiDiagnosticCacheEntry entry = new AiDiagnosticCacheEntry(
                1L, 10L, "hash-1", "gemini-2.5-flash", "v1", "{\"ok\":true}",
                nowUtc.minusHours(2), nowUtc.plusHours(22)
        );
        when(cachePort.findValidCache(10L, "hash-1", "gemini-2.5-flash", "v1", nowUtc))
                .thenReturn(Optional.of(entry));

        Optional<AiDiagnosticCacheEntry> result = createService()
                .findValidDiagnostic(10L, "hash-1", "gemini-2.5-flash", "v1");

        assertThat(result).contains(entry);
    }

    @Test
    void shouldReturnEmptyWhenCacheEntryIsExpiredAtBoundary() {
        LocalDateTime nowUtc = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        AiDiagnosticCacheEntry expiredAtExactNow = new AiDiagnosticCacheEntry(
                1L, 10L, "hash-1", "gemini-2.5-flash", "v1", "{\"ok\":true}",
                nowUtc.minusHours(24), nowUtc
        );
        when(cachePort.findValidCache(10L, "hash-1", "gemini-2.5-flash", "v1", nowUtc))
                .thenReturn(Optional.of(expiredAtExactNow));

        Optional<AiDiagnosticCacheEntry> result = createService()
                .findValidDiagnostic(10L, "hash-1", "gemini-2.5-flash", "v1");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOnCacheMissOrDifferentVersion() {
        LocalDateTime nowUtc = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        when(cachePort.findValidCache(10L, "hash-1", "gemini-2.5-flash", "v2", nowUtc))
                .thenReturn(Optional.empty());

        Optional<AiDiagnosticCacheEntry> result = createService()
                .findValidDiagnostic(10L, "hash-1", "gemini-2.5-flash", "v2");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveDiagnosticWith24hUtcExpiration() {
        LocalDateTime nowUtc = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        when(cachePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiDiagnosticCacheEntry saved = createService()
                .saveDiagnostic(10L, "hash-1", "gemini-2.5-flash", "v1", "{\"resumoExecutivo\":\"ok\"}");

        ArgumentCaptor<AiDiagnosticCacheEntry> captor = ArgumentCaptor.forClass(AiDiagnosticCacheEntry.class);
        verify(cachePort).save(captor.capture());

        AiDiagnosticCacheEntry captured = captor.getValue();
        assertThat(captured.createdAt()).isEqualTo(nowUtc);
        assertThat(captured.expiresAt()).isEqualTo(nowUtc.plusHours(24));
        assertThat(saved.expiresAt()).isEqualTo(nowUtc.plusHours(24));
        verify(cachePort).save(eq(captured));
    }

    @Test
    void shouldInvalidateCacheByUserIdAndIgnoreNull() {
        AiDiagnosticCacheService service = createService();

        service.invalidateByUserId(10L);
        verify(cachePort).deleteByUserId(10L);

        service.invalidateByUserId(null);
        verify(cachePort, never()).deleteByUserId(null);
    }
}
