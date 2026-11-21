package afsdigital.grahamselect.common.notifications.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebPushNotificationLogDto(
        UUID id,
        String ticker,
        String eventType,
        String title,
        String messagePreview,
        String provider,
        Integer httpStatus,
        String status,
        String errorMessage,
        OffsetDateTime sentAt
) {
}
