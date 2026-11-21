package afsdigital.grahamselect.api.notifications.web;

import afsdigital.grahamselect.common.notifications.application.dto.RegisterWebPushSubscriptionRequest;
import afsdigital.grahamselect.common.notifications.application.dto.UpdateWebPushPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushNotificationLogDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushSubscriptionDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushVapidKeyDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.usecase.DeleteWebPushSubscriptionUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWebPushNotificationHistoryUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWebPushPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.ListWebPushSubscriptionsUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.RegisterWebPushSubscriptionUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendTestWebPushAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.UpdateWebPushPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.WebPushMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/web-push")
@RequiredArgsConstructor
public class WebPushNotificationsController {

    private final WebPushGatewayPort gatewayPort;
    private final GetWebPushPreferencesUseCase getWebPushPreferencesUseCase;
    private final UpdateWebPushPreferencesUseCase updateWebPushPreferencesUseCase;
    private final RegisterWebPushSubscriptionUseCase registerWebPushSubscriptionUseCase;
    private final ListWebPushSubscriptionsUseCase listWebPushSubscriptionsUseCase;
    private final DeleteWebPushSubscriptionUseCase deleteWebPushSubscriptionUseCase;
    private final SendTestWebPushAlertUseCase sendTestWebPushAlertUseCase;
    private final GetWebPushNotificationHistoryUseCase getWebPushNotificationHistoryUseCase;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<WebPushVapidKeyDto> getVapidPublicKey(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("GET /api/v1/notifications/web-push/vapid-public-key - userId={}", userId);
        try {
            return ResponseEntity.ok(new WebPushVapidKeyDto(
                    gatewayPort.getVapidPublicKey(),
                    gatewayPort.getProviderName()
            ));
        } catch (Exception e) {
            log.error("Erro ao obter chave pública VAPID para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/preferences")
    public ResponseEntity<WebPushPreferenceDto> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("GET /api/v1/notifications/web-push/preferences - userId={}", userId);
        try {
            return ResponseEntity.ok(getWebPushPreferencesUseCase.execute(userId));
        } catch (Exception e) {
            log.error("Erro ao consultar preferências Web Push para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<WebPushPreferenceDto> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateWebPushPreferenceRequest request
    ) {
        String userId = extractUserId(jwt);
        log.info("PUT /api/v1/notifications/web-push/preferences - userId={}, optedIn={}, minMargin={}",
                userId, request != null && request.optedIn(), request != null ? request.minMarginOfSafetyPct() : null);
        try {
            return ResponseEntity.ok(updateWebPushPreferencesUseCase.execute(userId, request));
        } catch (Exception e) {
            log.error("Erro ao atualizar preferências Web Push para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<WebPushSubscriptionDto>> getSubscriptions(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("GET /api/v1/notifications/web-push/subscriptions - userId={}", userId);
        try {
            return ResponseEntity.ok(listWebPushSubscriptionsUseCase.execute(userId));
        } catch (Exception e) {
            log.error("Erro ao listar assinaturas Web Push para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<WebPushSubscriptionDto> registerSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RegisterWebPushSubscriptionRequest request
    ) {
        String userId = extractUserId(jwt);
        String maskedEndpoint = request != null ? WebPushMaskingUtils.maskEndpoint(request.endpoint()) : "****";
        log.info("POST /api/v1/notifications/web-push/subscriptions - userId={}, endpoint={}", userId, maskedEndpoint);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(registerWebPushSubscriptionUseCase.execute(userId, request));
        } catch (Exception e) {
            log.error("Erro ao registrar assinatura Web Push para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        String userId = extractUserId(jwt);
        log.info("DELETE /api/v1/notifications/web-push/subscriptions/{} - userId={}", id, userId);
        try {
            deleteWebPushSubscriptionUseCase.execute(userId, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao remover assinatura Web Push {} para userId={}: {}", id, userId, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/test")
    public ResponseEntity<WebPushTestAlertResultDto> sendTestAlert(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("POST /api/v1/notifications/web-push/test - userId={}", userId);
        try {
            return ResponseEntity.ok(sendTestWebPushAlertUseCase.execute(userId));
        } catch (Exception e) {
            log.error("Erro ao enviar alerta Web Push de teste para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<WebPushNotificationLogDto>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") int limit
    ) {
        String userId = extractUserId(jwt);
        log.info("GET /api/v1/notifications/web-push/history - userId={}, limit={}", userId, limit);
        try {
            return ResponseEntity.ok(getWebPushNotificationHistoryUseCase.execute(userId, limit));
        } catch (Exception e) {
            log.error("Erro ao consultar histórico Web Push para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    private String extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT ausente ou inválido.");
        }
        return jwt.getSubject();
    }
}
