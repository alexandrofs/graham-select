package afsdigital.grahamselect.common.notifications.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappNotificationLog {
    private UUID id;
    private String userId;
    private WhatsappAlertType eventType;
    private String dedupKey;
    private String messagePreview;
    private String provider;
    private String providerMessageId;
    private WhatsappDeliveryStatus status;
    private String errorMessage;
    private OffsetDateTime sentAt;
}
