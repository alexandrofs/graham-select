package afsdigital.grahamselect.common.notifications.application.dto;

import java.time.OffsetDateTime;

public record WhatsappTestAlertResultDto(
        String status,
        String provider,
        String providerMessageId,
        String messagePreview,
        OffsetDateTime sentAt
) {
}
