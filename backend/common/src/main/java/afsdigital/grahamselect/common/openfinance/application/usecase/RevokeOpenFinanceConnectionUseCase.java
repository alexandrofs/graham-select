package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import afsdigital.grahamselect.common.openfinance.application.repository.TokenEncryptionPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RevokeOpenFinanceConnectionUseCase {

    private final OpenFinanceConnectionPort connectionPort;
    private final OpenFinanceConnectorPort connectorPort;
    private final TokenEncryptionPort tokenEncryptionPort;

    public void execute(UUID connectionId, String userId) {
        log.info("Revogando conexão Open Finance: connectionId={}, userId={}", connectionId, userId);

        if (connectionId == null || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("connectionId e userId são obrigatórios.");
        }

        OpenFinanceConnection connection = connectionPort.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> {
                    log.error("Conexão não encontrada para revogação: connectionId={}, userId={}", connectionId, userId);
                    return new IllegalArgumentException("Conexão não encontrada.");
                });

        try {
            String accessToken = null;
            if (connection.getEncryptedAccessToken() != null) {
                accessToken = tokenEncryptionPort.decrypt(connection.getEncryptedAccessToken());
            }

            if (connection.getConsentId() != null) {
                log.info("Solicitando revogação remota de consentimento junto à instituição {}: consentId={}",
                        connection.getInstitutionId(), connection.getConsentId());
                connectorPort.revokeConsent(connection.getInstitutionId(), connection.getConsentId(), accessToken);
            }
        } catch (Exception e) {
            log.error("Aviso: Falha ao revogar consentimento remotamente na instituição {}: {}",
                    connection.getInstitutionId(), e.getMessage(), e);
            // Non-fatal: still revoke locally to guarantee user privacy and data purge
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        connection.setStatus(OpenFinanceConnectionStatus.REVOKED);
        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connection.setUpdatedAt(now);

        connectionPort.save(connection);
        log.info("Conexão Open Finance revogada com sucesso e credenciais limpas: connectionId={}", connectionId);
    }
}
