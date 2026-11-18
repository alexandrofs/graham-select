package afsdigital.grahamselect.api.openfinance.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessGuard;
import afsdigital.grahamselect.common.openfinance.application.dto.ConsentUrlResponseDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.ExchangeConsentCallbackDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.InitiateConnectionRequestDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceConnectionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceInstitutionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceSyncResultDTO;
import afsdigital.grahamselect.common.openfinance.application.usecase.CompleteOpenFinanceConsentUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.InitiateOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.ListOpenFinanceInstitutionsUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.ListUserOpenFinanceConnectionsUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.RevokeOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.SyncOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/open-finance")
@RequiredArgsConstructor
public class OpenFinanceController {

    private final ListOpenFinanceInstitutionsUseCase listInstitutionsUseCase;
    private final InitiateOpenFinanceConnectionUseCase initiateConnectionUseCase;
    private final CompleteOpenFinanceConsentUseCase completeConsentUseCase;
    private final SyncOpenFinanceConnectionUseCase syncConnectionUseCase;
    private final RevokeOpenFinanceConnectionUseCase revokeConnectionUseCase;
    private final ListUserOpenFinanceConnectionsUseCase listUserConnectionsUseCase;
    private final PremiumFeatureAccessGuard premiumFeatureAccessGuard;

    @Value("${openfinance.webhook.secret:sandbox-openfinance-webhook-secret-2026}")
    private String configuredWebhookSecret;

    @GetMapping("/institutions")
    public ResponseEntity<List<OpenFinanceInstitutionDTO>> listInstitutions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        log.info("Recebida requisição GET /api/v1/open-finance/institutions: userId={}", userId);
        List<OpenFinanceInstitutionDTO> institutions = listInstitutionsUseCase.execute();
        return ResponseEntity.ok(institutions);
    }

    @GetMapping("/connections")
    public ResponseEntity<List<OpenFinanceConnectionDTO>> listConnections(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("Recebida requisição GET /api/v1/open-finance/connections: userId={}", userId);
        List<OpenFinanceConnectionDTO> connections = listUserConnectionsUseCase.execute(userId);
        return ResponseEntity.ok(connections);
    }

    @PostMapping("/connections")
    public ResponseEntity<ConsentUrlResponseDTO> initiateConnection(
            @RequestBody InitiateConnectionRequestDTO request,
            @RequestParam(value = "redirectUri", required = false) String redirectUri,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("Recebida requisição POST /api/v1/open-finance/connections: userId={}, institutionId={}, profileId={}",
                userId, request != null ? request.institutionId() : null, request != null ? request.profileId() : null);

        if (request != null && request.profileId() != null) {
            if (!premiumFeatureAccessGuard.canAccessPremiumFeatures()) {
                log.warn("Tentativa de conectar perfil familiar Open Finance sem plano Premium: userId={}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        try {
            ConsentUrlResponseDTO response = initiateConnectionUseCase.execute(userId, request, redirectUri);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Erro na iniciação de conexão Open Finance: userId={}, error={}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro inesperado na iniciação de conexão Open Finance: userId={}, error={}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao iniciar conexão Open Finance: " + e.getMessage(), e);
        }
    }

    @PostMapping("/connections/callback")
    public ResponseEntity<OpenFinanceConnectionDTO> completeConsentCallback(
            @RequestBody ExchangeConsentCallbackDTO callback,
            @RequestParam(value = "redirectUri", required = false) String redirectUri,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("Recebida requisição POST /api/v1/open-finance/connections/callback: userId={}, connectionId={}",
                userId, callback != null ? callback.connectionId() : null);

        try {
            OpenFinanceConnectionDTO response = completeConsentUseCase.execute(userId, callback, redirectUri);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Parâmetros inválidos no callback de consentimento: userId={}, error={}", userId, e.getMessage(), e);
            HttpStatus status = (e.getMessage() != null && e.getMessage().contains("não encontrada")) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status, e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("Falha no consentimento junto à instituição: userId={}, error={}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro no processamento do callback de consentimento: userId={}, error={}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao processar callback de consentimento: " + e.getMessage(), e);
        }
    }

    @PostMapping("/connections/{id}/sync")
    public ResponseEntity<OpenFinanceSyncResultDTO> syncConnection(
            @PathVariable("id") UUID connectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("Recebida requisição POST /api/v1/open-finance/connections/{}/sync: userId={}", connectionId, userId);

        try {
            OpenFinanceSyncResultDTO result = syncConnectionUseCase.execute(connectionId, userId, OpenFinanceSyncType.MANUAL);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Conexão não encontrada para sincronização manual: connectionId={}, userId={}, error={}",
                    connectionId, userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("Estado inválido para sincronização: connectionId={}, userId={}, error={}",
                    connectionId, userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro na sincronização manual da conexão Open Finance: connectionId={}, userId={}, error={}",
                    connectionId, userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao sincronizar conexão Open Finance: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> revokeConnection(
            @PathVariable("id") UUID connectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("Recebida requisição DELETE /api/v1/open-finance/connections/{}: userId={}", connectionId, userId);

        try {
            revokeConnectionUseCase.execute(connectionId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Conexão não encontrada para exclusão/revogação: connectionId={}, userId={}, error={}",
                    connectionId, userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro ao revogar conexão Open Finance: connectionId={}, userId={}, error={}",
                    connectionId, userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao revogar conexão Open Finance: " + e.getMessage(), e);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        log.info("Recebida requisição POST /api/v1/open-finance/webhook: payloadSize={}",
                payload != null ? payload.size() : 0);

        byte[] provided = webhookSecret != null ? webhookSecret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] expected = configuredWebhookSecret.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(provided, expected)) {
            log.error("Acesso não autorizado ao webhook Open Finance: secret recebido inválido ou ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Segredo de webhook inválido"));
        }

        if (payload != null && !payload.isEmpty()) {
            log.info("Processando evento de webhook Open Finance: event={}, connectionId={}",
                    payload.get("event"), payload.get("connectionId"));
            if ("CONSENT_REVOKED".equalsIgnoreCase(String.valueOf(payload.get("event"))) && payload.get("connectionId") != null) {
                try {
                    UUID connId = UUID.fromString(String.valueOf(payload.get("connectionId")));
                    String targetUserId = payload.get("userId") != null ? String.valueOf(payload.get("userId")) : null;
                    if (targetUserId != null) {
                        revokeConnectionUseCase.execute(connId, targetUserId);
                        log.info("Consentimento revogado com sucesso via Webhook: connectionId={}", connId);
                    }
                } catch (Exception ex) {
                    log.error("Erro ao processar revogação via Webhook: error={}", ex.getMessage(), ex);
                }
            }
        }

        log.info("Webhook Open Finance autenticado e processado com sucesso.");
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
}
