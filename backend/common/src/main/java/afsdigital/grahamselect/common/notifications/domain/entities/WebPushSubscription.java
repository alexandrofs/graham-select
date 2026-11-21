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
public class WebPushSubscription {
    private UUID id;
    private String userId;
    private String endpoint;
    private String endpointHash;
    private String p256dhKey;
    private String authKey;
    private String userAgent;
    private boolean active;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
