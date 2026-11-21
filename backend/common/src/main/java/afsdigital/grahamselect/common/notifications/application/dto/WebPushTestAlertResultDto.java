package afsdigital.grahamselect.common.notifications.application.dto;

import java.time.OffsetDateTime;

public record WebPushTestAlertResultDto(
        int sentCount,
        int failedCount,
        String provider,
        String messagePreview,
        OffsetDateTime sentAt
) {
}
