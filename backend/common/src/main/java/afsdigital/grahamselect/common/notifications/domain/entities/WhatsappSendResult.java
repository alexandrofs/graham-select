package afsdigital.grahamselect.common.notifications.domain.entities;

public record WhatsappSendResult(
        boolean success,
        String provider,
        String providerMessageId,
        String errorMessage
) {
}
