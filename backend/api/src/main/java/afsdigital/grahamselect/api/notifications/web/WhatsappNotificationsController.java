package afsdigital.grahamselect.api.notifications.web;

import afsdigital.grahamselect.common.notifications.application.dto.UpdateWhatsappPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappNotificationLogDto;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWhatsappNotificationHistoryUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWhatsappPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendTestWhatsappAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.UpdateWhatsappPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.WhatsappMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/whatsapp")
@RequiredArgsConstructor
public class WhatsappNotificationsController {

    private final GetWhatsappPreferencesUseCase getWhatsappPreferencesUseCase;
    private final UpdateWhatsappPreferencesUseCase updateWhatsappPreferencesUseCase;
    private final SendTestWhatsappAlertUseCase sendTestWhatsappAlertUseCase;
    private final GetWhatsappNotificationHistoryUseCase getWhatsappNotificationHistoryUseCase;

    @GetMapping("/preferences")
    public ResponseEntity<WhatsappPreferenceDto> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("GET /api/v1/notifications/whatsapp/preferences - userId={}", userId);
        try {
            return ResponseEntity.ok(getWhatsappPreferencesUseCase.execute(userId));
        } catch (Exception e) {
            log.error("Erro ao consultar preferências WhatsApp para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<WhatsappPreferenceDto> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateWhatsappPreferenceRequest request
    ) {
        String userId = extractUserId(jwt);
        String maskedPhone = request != null ? WhatsappMaskingUtils.maskPhoneNumber(request.phoneNumber()) : "****";
        log.info("PUT /api/v1/notifications/whatsapp/preferences - userId={}, phone={}, optedIn={}",
                userId, maskedPhone, request != null && request.optedIn());
        try {
            return ResponseEntity.ok(updateWhatsappPreferencesUseCase.execute(userId, request));
        } catch (Exception e) {
            log.error("Erro ao atualizar preferências WhatsApp para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/test")
    public ResponseEntity<WhatsappTestAlertResultDto> sendTestAlert(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("POST /api/v1/notifications/whatsapp/test - userId={}", userId);
        try {
            return ResponseEntity.ok(sendTestWhatsappAlertUseCase.execute(userId));
        } catch (Exception e) {
            log.error("Erro ao enviar alerta WhatsApp de teste para userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<WhatsappNotificationLogDto>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") int limit
    ) {
        String userId = extractUserId(jwt);
        log.info("GET /api/v1/notifications/whatsapp/history - userId={}, limit={}", userId, limit);
        try {
            return ResponseEntity.ok(getWhatsappNotificationHistoryUseCase.execute(userId, limit));
        } catch (Exception e) {
            log.error("Erro ao consultar histórico WhatsApp para userId={}: {}", userId, e.getMessage(), e);
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
