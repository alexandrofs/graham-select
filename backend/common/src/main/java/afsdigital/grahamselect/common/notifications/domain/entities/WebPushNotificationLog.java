package afsdigital.grahamselect.common.notifications.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebPushNotificationLog {
    private UUID id;
    private String userId;
    private UUID subscriptionId;
    private String ticker;
    private WebPushAlertType eventType;
    private String dedupKey;
    private String title;
    private String messagePreview;
    private String provider;
    private Integer httpStatus;
    private WebPushDeliveryStatus status;
    private String errorMessage;
    private OffsetDateTime sentAt;
}
