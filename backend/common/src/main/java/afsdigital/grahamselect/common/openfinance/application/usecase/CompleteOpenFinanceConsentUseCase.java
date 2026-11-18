package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.openfinance.application.dto.ExchangeConsentCallbackDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceConnectionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTokenPairDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import afsdigital.grahamselect.common.openfinance.application.repository.TokenEncryptionPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class CompleteOpenFinanceConsentUseCase {

    private final OpenFinanceConnectionPort connectionPort;
    private final OpenFinanceConnectorPort connectorPort;
    private final TokenEncryptionPort tokenEncryptionPort;
    private final SyncOpenFinanceConnectionUseCase syncConnectionUseCase;

    public CompleteOpenFinanceConsentUseCase(
            OpenFinanceConnectionPort connectionPort,
            OpenFinanceConnectorPort connectorPort,
            TokenEncryptionPort tokenEncryptionPort
    ) {
        this(connectionPort, connectorPort, tokenEncryptionPort, null);
    }

    public CompleteOpenFinanceConsentUseCase(
            OpenFinanceConnectionPort connectionPort,
            OpenFinanceConnectorPort connectorPort,
            TokenEncryptionPort tokenEncryptionPort,
            SyncOpenFinanceConnectionUseCase syncConnectionUseCase
    ) {
        this.connectionPort = connectionPort;
        this.connectorPort = connectorPort;
        this.tokenEncryptionPort = tokenEncryptionPort;
        this.syncConnectionUseCase = syncConnectionUseCase;
    }

    public OpenFinanceConnectionDTO execute(String userId, ExchangeConsentCallbackDTO callback, String redirectUri) {
        log.info("Processando callback de consentimento Open Finance: userId={}, connectionId={}",
                userId, callback != null ? callback.connectionId() : null);

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId é obrigatório.");
        }
        if (callback == null || callback.connectionId() == null || callback.code() == null || callback.code().isBlank()) {
            throw new IllegalArgumentException("connectionId e code são obrigatórios.");
        }

        OpenFinanceConnection connection = connectionPort.findByIdAndUserId(callback.connectionId(), userId)
                .orElseThrow(() -> {
                    log.error("Conexão Open Finance não encontrada para callback: userId={}, connectionId={}",
                            userId, callback.connectionId());
                    return new IllegalArgumentException("Conexão não encontrada.");
                });

        if (connection.getConsentId() != null && !connection.getConsentId().equals(callback.state())) {
            log.error("Falha de validação CSRF/State no callback Open Finance: esperado={}, recebido={}",
                    connection.getConsentId(), callback.state());
            throw new IllegalArgumentException("State de consentimento inválido ou divergente.");
        }

        try {
            OpenFinanceTokenPairDTO tokenPair = connectorPort.exchangeConsentCode(
                    connection.getInstitutionId(),
                    callback.code(),
                    callback.state(),
                    redirectUri
            );

            if (tokenPair == null || tokenPair.accessToken() == null || tokenPair.accessToken().isBlank()) {
                throw new IllegalStateException("Instituição não retornou tokens de acesso válidos.");
            }

            String encryptedAccess = tokenEncryptionPort.encrypt(tokenPair.accessToken());
            String encryptedRefresh = tokenPair.refreshToken() != null
                    ? tokenEncryptionPort.encrypt(tokenPair.refreshToken())
                    : null;

            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            connection.setStatus(OpenFinanceConnectionStatus.ACTIVE);
            connection.setConsentId(tokenPair.consentId() != null ? tokenPair.consentId() : connection.getConsentId());
            connection.setEncryptedAccessToken(encryptedAccess);
            connection.setEncryptedRefreshToken(encryptedRefresh);
            connection.setConsentExpiresAt(tokenPair.expiresAt());
            connection.setErrorMessage(null);
            connection.setUpdatedAt(now);

            OpenFinanceConnection saved = connectionPort.save(connection);
            log.info("Conexão Open Finance ativada com sucesso: connectionId={}, userId={}, institutionId={}",
                    saved.getId(), userId, saved.getInstitutionId());

            if (syncConnectionUseCase != null) {
                try {
                    log.info("Disparando sincronização inicial pós-consentimento: connectionId={}, userId={}", saved.getId(), userId);
                    syncConnectionUseCase.execute(saved.getId(), userId, OpenFinanceSyncType.INITIAL);
                } catch (Exception syncEx) {
                    log.error("Erro ao executar sincronização inicial pós-consentimento: connectionId={}, error={}",
                            saved.getId(), syncEx.getMessage(), syncEx);
                }
            }

            return OpenFinanceConnectionDTO.fromDomain(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao trocar código de consentimento por tokens: connectionId={}, userId={}, error={}",
                    connection.getId(), userId, e.getMessage(), e);
            connection.setStatus(OpenFinanceConnectionStatus.FAILED);
            connection.setErrorMessage(e.getMessage());
            connection.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            connectionPort.save(connection);
            throw new IllegalStateException("Falha ao autorizar consentimento junto à instituição: " + e.getMessage(), e);
        }
    }
}
