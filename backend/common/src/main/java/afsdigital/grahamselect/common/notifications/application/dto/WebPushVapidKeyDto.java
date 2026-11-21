package afsdigital.grahamselect.common.notifications.application.dto;

public record WebPushVapidKeyDto(
        String publicKey,
        String provider
) {
}
