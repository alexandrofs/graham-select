package afsdigital.grahamselect.common.notifications.domain.entities;

public record WebPushSendResult(
        boolean success,
        boolean expired,
        Integer httpStatus,
        String provider,
        String errorMessage
) {
}
