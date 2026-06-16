package afsdigital.grahamselect.common.domain.entities;

import java.util.UUID;

public record ValuationFailedEvent(
    String userId,
    String errorCode,
    String message,
    String failedAt,
    UUID eventId
) {}
