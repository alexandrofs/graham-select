package afsdigital.grahamselect.common.notifications.application.dto;

public record RegisterWebPushSubscriptionRequest(
        String endpoint,
        String p256dhKey,
        String authKey,
        String userAgent
) {
}
