package afsdigital.grahamselect.common.portfolio.domain.events;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ValuationRequestedEvent(
        String eventId,
        String version,
        OffsetDateTime timestamp,
        String userId,
        String ticker
) {
}
