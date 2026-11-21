package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WebPushTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SendTestWebPushAlertUseCase {

    private final WebPushPreferencePort preferencePort;
    private final WebPushSubscriptionPort subscriptionPort;
    private final SendWebPushAlertUseCase sendWebPushAlertUseCase;
    private final WebPushGatewayPort gatewayPort;

    public WebPushTestAlertResultDto execute(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }

        log.info("Disparando alerta de teste Web Push para userId={}", userId);

        WebPushPreference preference = preferencePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não possui consentimento ativo para notificações Web Push."));

        if (!preference.isOptedIn()) {
            throw new IllegalArgumentException("Usuário não possui consentimento ativo para notificações Web Push.");
        }

        List<WebPushSubscription> activeSubscriptions = subscriptionPort.findActiveByUserId(userId);
        if (activeSubscriptions.isEmpty()) {
            throw new IllegalArgumentException("Nenhum navegador/dispositivo ativo registrado para receber notificações Web Push.");
        }

        String title = "Teste de Notificação Web Push";
        String message = "Seu navegador está configurado para receber alertas de compra Graham!";
        String url = "/notifications/web-push";

        int sentCount = 0;
        int failedCount = 0;

        for (WebPushSubscription sub : activeSubscriptions) {
            String dedupKey = "test-alert:" + userId + ":" + sub.getId() + ":" + System.currentTimeMillis();
            boolean success = sendWebPushAlertUseCase.sendAlert(
                    userId, sub, null, WebPushAlertType.TEST_ALERT,
                    dedupKey, title, message, url
            );
            if (success) {
                sentCount++;
            } else {
                failedCount++;
            }
        }

        return new WebPushTestAlertResultDto(
                sentCount,
                failedCount,
                gatewayPort.getProviderName(),
                message,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
