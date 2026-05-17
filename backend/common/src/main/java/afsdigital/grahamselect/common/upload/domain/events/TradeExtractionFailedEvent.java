package afsdigital.grahamselect.common.upload.domain.events;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Map;

@Builder
public record TradeExtractionFailedEvent(
        String eventId,
        String version,
        OffsetDateTime timestamp,
        String userId,
        String correlationId,
        String fileName,
        long lineNumber,
        Map<String, String> originalPayload,
        String reason
) {
}
