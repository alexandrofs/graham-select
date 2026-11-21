package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappDeliveryStatus;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationLog;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SendWhatsappAlertUseCase {

    private final WhatsappNotificationLogPort logPort;
    private final WhatsappGatewayPort gatewayPort;

    public boolean sendAlert(
            WhatsappNotificationPreference preference,
            WhatsappAlertType eventType,
            String dedupKey,
            String message
    ) {
        if (preference == null || !preference.isOptedIn()
                || preference.getPhoneNumber() == null || preference.getPhoneNumber().isBlank()) {
            return false;
        }
        if (dedupKey == null || dedupKey.isBlank()) {
            throw new IllegalArgumentException("dedupKey cannot be null or blank");
        }
        String boundedDedupKey = WhatsappMaskingUtils.boundDedupKey(dedupKey, 255);
        var existingOpt = logPort.findByDedupKey(boundedDedupKey);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == WhatsappDeliveryStatus.SENT) {
            log.debug("Alerta WhatsApp ignorado por idempotência: userId={}, dedupKey={}", preference.getUserId(), boundedDedupKey);
            return false;
        }

        String maskedPhone = WhatsappMaskingUtils.maskPhoneNumber(preference.getPhoneNumber());
        log.info("Disparando alerta WhatsApp userId={}, phone={}, eventType={}, dedupKey={}",
                preference.getUserId(), maskedPhone, eventType, boundedDedupKey);

        WhatsappSendResult result;
        try {
            result = gatewayPort.sendTextMessage(preference.getPhoneNumber(), message);
            if (result == null) {
                result = new WhatsappSendResult(false, "UNKNOWN", null, "Gateway retornou resposta nula");
            }
        } catch (Exception e) {
            log.error("Falha inesperada ao enviar alerta WhatsApp userId={}, phone={}, eventType={}, dedupKey={}: {}",
                    preference.getUserId(), maskedPhone, eventType, boundedDedupKey, e.getMessage(), e);
            result = new WhatsappSendResult(false, "UNKNOWN", null, e.getMessage());
        }

        WhatsappDeliveryStatus status = result.success() ? WhatsappDeliveryStatus.SENT : WhatsappDeliveryStatus.FAILED;
        String provider = WhatsappMaskingUtils.truncate(
                result.provider() != null && !result.provider().isBlank() ? result.provider() : "UNKNOWN",
                50
        );
        String providerMessageId = result.providerMessageId() != null
                ? WhatsappMaskingUtils.truncate(result.providerMessageId(), 128)
                : null;

        WhatsappNotificationLog entry = WhatsappNotificationLog.builder()
                .id(existingOpt.map(WhatsappNotificationLog::getId).orElseGet(UUID::randomUUID))
                .userId(preference.getUserId())
                .eventType(eventType)
                .dedupKey(boundedDedupKey)
                .messagePreview(WhatsappMaskingUtils.truncate(message, 500))
                .provider(provider)
                .providerMessageId(providerMessageId)
                .status(status)
                .errorMessage(result.errorMessage())
                .sentAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        try {
            logPort.save(entry);
        } catch (RuntimeException e) {
            log.error("Falha ao persistir log de alerta WhatsApp userId={}, dedupKey={}: {}",
                    preference.getUserId(), boundedDedupKey, e.getMessage(), e);
            return false;
        }
        return result.success();
    }
}
