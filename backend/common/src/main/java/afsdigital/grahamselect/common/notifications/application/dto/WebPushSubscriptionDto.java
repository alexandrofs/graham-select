package afsdigital.grahamselect.common.notifications.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebPushSubscriptionDto(
        UUID id,
        String endpointPreview,
        String userAgent,
        boolean active,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt
) {
}
