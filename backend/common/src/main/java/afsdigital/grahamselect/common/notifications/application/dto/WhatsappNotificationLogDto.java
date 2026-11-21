package afsdigital.grahamselect.common.notifications.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappNotificationLogDto(
        UUID id,
        String eventType,
        String messagePreview,
        String provider,
        String status,
        String errorMessage,
        OffsetDateTime sentAt
) {
}
