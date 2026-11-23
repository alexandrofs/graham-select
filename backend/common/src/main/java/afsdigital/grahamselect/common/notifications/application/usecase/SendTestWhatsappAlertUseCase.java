package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WhatsappTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
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
public class SendTestWhatsappAlertUseCase {

    private final WhatsappPreferencePort preferencePort;
    private final WhatsappNotificationLogPort logPort;
    private final WhatsappGatewayPort gatewayPort;

    public WhatsappTestAlertResultDto execute(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }

        WhatsappNotificationPreference preference = preferencePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Configure seu número de WhatsApp e ative o consentimento (opt-in) antes de enviar um teste."));

        if (!preference.isOptedIn() || preference.getPhoneNumber() == null || preference.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Configure seu número de WhatsApp e ative o consentimento (opt-in) antes de enviar um teste.");
        }

        String maskedPhone = WhatsappMaskingUtils.maskPhoneNumber(preference.getPhoneNumber());
        String message = "Kerno: Alerta de teste configurado com sucesso! Seus gatilhos de carteira estão ativos.";
        log.info("Enviando alerta WhatsApp de teste para userId={}, phone={}", userId, maskedPhone);

        WhatsappSendResult result;
        try {
            result = gatewayPort.sendTextMessage(preference.getPhoneNumber(), message);
            if (result == null) {
                result = new WhatsappSendResult(false, "UNKNOWN", null, "Gateway retornou resposta nula");
            }
        } catch (Exception e) {
            log.error("Erro ao enviar alerta WhatsApp de teste para userId={}, phone={}: {}",
                    userId, maskedPhone, e.getMessage(), e);
            result = new WhatsappSendResult(false, "UNKNOWN", null, e.getMessage());
        }

        OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
        WhatsappDeliveryStatus status = result.success() ? WhatsappDeliveryStatus.SENT : WhatsappDeliveryStatus.FAILED;
        String provider = result.provider() != null && !result.provider().isBlank() ? result.provider() : "UNKNOWN";
        String preview = WhatsappMaskingUtils.truncate(message, 500);

        WhatsappNotificationLog logEntry = WhatsappNotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .eventType(WhatsappAlertType.TEST)
                .dedupKey("test:" + userId + ":" + UUID.randomUUID())
                .messagePreview(preview)
                .provider(provider)
                .providerMessageId(result.providerMessageId())
                .status(status)
                .errorMessage(result.errorMessage())
                .sentAt(sentAt)
                .build();

        logPort.save(logEntry);

        return new WhatsappTestAlertResultDto(
                status.name(),
                provider,
                result.providerMessageId(),
                preview,
                sentAt
        );
    }
}
