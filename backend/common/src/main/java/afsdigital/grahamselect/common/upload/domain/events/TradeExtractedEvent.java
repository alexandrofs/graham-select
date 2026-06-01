package afsdigital.grahamselect.common.upload.domain.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
public record TradeExtractedEvent(
        String eventId,
        String version,
        OffsetDateTime timestamp,
        String userId,
        String correlationId,
        String fileName,
        long lineNumber,
        String ticker,
        LocalDate tradeDate,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        String broker
) {
}
