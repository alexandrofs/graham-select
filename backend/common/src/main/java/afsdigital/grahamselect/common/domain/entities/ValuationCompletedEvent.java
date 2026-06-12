package afsdigital.grahamselect.common.domain.entities;

import java.util.UUID;

public record ValuationCompletedEvent(
    String userId,
    int recommendationCount,
    String completedAt,
    UUID eventId
) {}
