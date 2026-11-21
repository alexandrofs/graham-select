package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushDeliveryStatus;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushNotificationLog;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SendWebPushAlertUseCase {

    private final WebPushNotificationLogPort logPort;
    private final WebPushSubscriptionPort subscriptionPort;
    private final WebPushGatewayPort gatewayPort;

    public boolean sendAlert(
            String userId,
            WebPushSubscription subscription,
            String ticker,
            WebPushAlertType eventType,
            String dedupKey,
            String title,
            String message,
            String url
    ) {
        if (subscription == null || !subscription.isActive()
                || subscription.getEndpoint() == null || subscription.getEndpoint().isBlank()) {
            return false;
        }
        if (dedupKey == null || dedupKey.isBlank()) {
            throw new IllegalArgumentException("dedupKey cannot be null or blank");
        }

        String boundedDedupKey = WebPushMaskingUtils.boundDedupKey(dedupKey, 255);
        var existingOpt = logPort.findByDedupKey(boundedDedupKey);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == WebPushDeliveryStatus.SENT) {
            log.debug("Alerta Web Push ignorado por idempotência: userId={}, dedupKey={}", userId, boundedDedupKey);
            return false;
        }

        String maskedEndpoint = WebPushMaskingUtils.maskEndpoint(subscription.getEndpoint());
        log.info("Disparando alerta Web Push userId={}, endpoint={}, eventType={}, dedupKey={}",
                userId, maskedEndpoint, eventType, boundedDedupKey);

        WebPushSendResult result;
        try {
            result = gatewayPort.sendNotification(subscription, title, message, url);
            if (result == null) {
                result = new WebPushSendResult(false, false, 500, "UNKNOWN", "Gateway retornou resposta nula");
            }
        } catch (Exception e) {
            log.error("Falha inesperada ao enviar alerta Web Push userId={}, endpoint={}, eventType={}, dedupKey={}: {}",
                    userId, maskedEndpoint, eventType, boundedDedupKey, e.getMessage(), e);
            result = new WebPushSendResult(false, false, 500, "UNKNOWN", e.getMessage());
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        WebPushDeliveryStatus status;
        if (result.expired()) {
            status = WebPushDeliveryStatus.EXPIRED;
            subscription.setActive(false);
            subscription.setUpdatedAt(now);
            try {
                subscriptionPort.save(subscription);
                log.info("Assinatura Web Push desativada devido a endpoint expirado: id={}, endpoint={}",
                        subscription.getId(), maskedEndpoint);
            } catch (Exception e) {
                log.error("Erro ao salvar desativação de assinatura Web Push expirada {}: {}",
                        subscription.getId(), e.getMessage(), e);
            }
        } else if (result.success()) {
            status = WebPushDeliveryStatus.SENT;
            subscription.setLastUsedAt(now);
            subscription.setUpdatedAt(now);
            try {
                subscriptionPort.save(subscription);
            } catch (Exception e) {
                log.error("Erro ao atualizar lastUsedAt da assinatura {}: {}", subscription.getId(), e.getMessage(), e);
            }
        } else {
            status = WebPushDeliveryStatus.FAILED;
        }

        String provider = WebPushMaskingUtils.truncate(
                result.provider() != null && !result.provider().isBlank() ? result.provider() : "UNKNOWN",
                50
        );

        WebPushNotificationLog entry = WebPushNotificationLog.builder()
                .id(existingOpt.map(WebPushNotificationLog::getId).orElseGet(UUID::randomUUID))
                .userId(userId)
                .subscriptionId(subscription.getId())
                .ticker(ticker != null ? WebPushMaskingUtils.truncate(ticker.trim().toUpperCase(), 20) : null)
                .eventType(eventType)
                .dedupKey(boundedDedupKey)
                .title(WebPushMaskingUtils.truncate(title, 150))
                .messagePreview(WebPushMaskingUtils.truncate(message, 500))
                .provider(provider)
                .httpStatus(result.httpStatus())
                .status(status)
                .errorMessage(result.errorMessage())
                .sentAt(now)
                .build();

        try {
            logPort.save(entry);
        } catch (RuntimeException e) {
            log.error("Falha ao persistir log de alerta Web Push userId={}, dedupKey={}: {}",
                    userId, boundedDedupKey, e.getMessage(), e);
            return false;
        }

        return result.success();
    }
}
