package afsdigital.grahamselect.common.domain.entities;

import java.util.UUID;

public record ValuationRequestedEvent(
    String userId,
    String requestedAt,
    UUID eventId
) {}
