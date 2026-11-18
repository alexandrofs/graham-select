package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinancePositionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceSyncResultDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTokenPairDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTransactionDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceSyncLogPort;
import afsdigital.grahamselect.common.openfinance.application.repository.TokenEncryptionPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncLog;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SendPortfolioUpdateNotificationUseCase;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SyncOpenFinanceConnectionUseCase {

    private final OpenFinanceConnectionPort connectionPort;
    private final OpenFinanceSyncLogPort syncLogPort;
    private final OpenFinanceConnectorPort connectorPort;
    private final TokenEncryptionPort tokenEncryptionPort;
    private final SaveTradeUseCase saveTradeUseCase;
    private final SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase;

    public OpenFinanceSyncResultDTO execute(UUID connectionId, String userId, OpenFinanceSyncType syncType) {
        log.info("Iniciando sincronização Open Finance: connectionId={}, userId={}, syncType={}",
                connectionId, userId, syncType);

        if (connectionId == null) {
            throw new IllegalArgumentException("connectionId é obrigatório.");
        }

        OpenFinanceConnection connection = (userId != null && !userId.isBlank())
                ? connectionPort.findByIdAndUserId(connectionId, userId).orElse(null)
                : connectionPort.findById(connectionId).orElse(null);

        if (connection == null) {
            log.error("Conexão Open Finance não encontrada para sincronização: connectionId={}, userId={}",
                    connectionId, userId);
            throw new IllegalArgumentException("Conexão Open Finance não encontrada.");
        }

        if (connection.getStatus() == OpenFinanceConnectionStatus.REVOKED) {
            log.error("Tentativa de sincronizar conexão revogada: connectionId={}", connectionId);
            throw new IllegalStateException("Conexão Open Finance revogada pelo usuário.");
        }
        if (connection.getStatus() == OpenFinanceConnectionStatus.SYNCING) {
            log.warn("Sincronização já em andamento para a conexão: connectionId={}", connectionId);
            throw new IllegalStateException("Sincronização em andamento para esta conexão.");
        }

        LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC);
        OpenFinanceSyncLog syncLog = OpenFinanceSyncLog.builder()
                .id(UUID.randomUUID())
                .connectionId(connection.getId())
                .userId(connection.getUserId())
                .startedAt(startedAt)
                .syncType(syncType)
                .status(OpenFinanceSyncStatus.RUNNING)
                .createdAt(startedAt)
                .build();
        syncLogPort.save(syncLog);

        connection.setStatus(OpenFinanceConnectionStatus.SYNCING);
        connection.setUpdatedAt(startedAt);
        connectionPort.save(connection);

        try {
            String accessToken = tokenEncryptionPort.decrypt(connection.getEncryptedAccessToken());
            String refreshToken = tokenEncryptionPort.decrypt(connection.getEncryptedRefreshToken());

            boolean isExpiredOrNear = connection.getConsentExpiresAt() != null
                    && connection.getConsentExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

            if ((accessToken == null || accessToken.isBlank() || isExpiredOrNear) && refreshToken != null) {
                log.info("Token de acesso expirado ou próximo da expiração. Renovando token: connectionId={}", connectionId);
                try {
                    OpenFinanceTokenPairDTO refreshed = connectorPort.refreshToken(connection.getInstitutionId(), refreshToken);
                    if (refreshed != null && refreshed.accessToken() != null) {
                        accessToken = refreshed.accessToken();
                        connection.setEncryptedAccessToken(tokenEncryptionPort.encrypt(refreshed.accessToken()));
                        if (refreshed.refreshToken() != null) {
                            connection.setEncryptedRefreshToken(tokenEncryptionPort.encrypt(refreshed.refreshToken()));
                        }
                        if (refreshed.expiresAt() != null) {
                            connection.setConsentExpiresAt(refreshed.expiresAt());
                        }
                        connection.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                        connectionPort.save(connection);
                    }
                } catch (Exception refreshEx) {
                    log.error("Falha ao renovar token de acesso Open Finance: connectionId={}, error={}",
                            connectionId, refreshEx.getMessage(), refreshEx);
                }
            }

            if ((accessToken == null || accessToken.isBlank()) && refreshToken == null) {
                log.error("Credenciais de acesso inexistentes para conexão Open Finance: connectionId={}", connectionId);
                throw new IllegalStateException("Credenciais de acesso não encontradas.");
            }

            List<OpenFinancePositionDTO> positions = connectorPort.fetchPositions(connection.getInstitutionId(), accessToken);
            LocalDate fromDate = connection.getLastSyncAt() != null
                    ? connection.getLastSyncAt().toLocalDate()
                    : LocalDate.now(ZoneOffset.UTC).minusYears(1);
            LocalDate toDate = LocalDate.now(ZoneOffset.UTC);
            List<OpenFinanceTransactionDTO> transactions = connectorPort.fetchTransactions(
                    connection.getInstitutionId(), accessToken, fromDate, toDate
            );

            int tradesCreated = 0;
            int tradesDuplicated = 0;

            if (transactions != null) {
                for (OpenFinanceTransactionDTO tx : transactions) {
                    String sideNormalized = normalizeTradeSide(tx.side());
                    if (sideNormalized == null) {
                        log.debug("Ignorando lançamento não operacional Open Finance: txId={}, memo={}", tx.transactionId(), tx.memo());
                        continue;
                    }
                    if (tx.ticker() == null || tx.ticker().isBlank()) {
                        log.debug("Ignorando transação Open Finance sem ticker válido: txId={}", tx.transactionId());
                        continue;
                    }

                    try {
                        Trade trade = Trade.builder()
                                .userId(connection.getUserId())
                                .profileId(connection.getProfileId() != null ? connection.getProfileId().toString() : null)
                                .ticker(tx.ticker().toUpperCase(Locale.ROOT).trim())
                                .tradeDate(tx.tradeDate() != null ? tx.tradeDate() : LocalDate.now(ZoneOffset.UTC))
                                .quantity(tx.quantity())
                                .price(tx.price())
                                .broker(connection.getInstitutionName())
                                .side(sideNormalized)
                                .build();

                        Trade savedTrade = saveTradeUseCase.execute(trade);
                        if (savedTrade != null) {
                            tradesCreated++;
                        } else {
                            tradesDuplicated++;
                        }
                    } catch (Exception tradeEx) {
                        log.error("Erro ao persistir trade individual Open Finance: txId={}, ticker={}, error={}",
                                tx.transactionId(), tx.ticker(), tradeEx.getMessage(), tradeEx);
                    }
                }
            }

            if (tradesCreated > 0 && sendPortfolioUpdateNotificationUseCase != null) {
                log.info("Disparando notificação em tempo real SSE após ingestão de {} novos trades Open Finance: userId={}",
                        tradesCreated, connection.getUserId());
                sendPortfolioUpdateNotificationUseCase.execute(connection.getUserId());
            }

            LocalDateTime completedAt = LocalDateTime.now(ZoneOffset.UTC);
            syncLog.setCompletedAt(completedAt);
            syncLog.setPositionsFetched(positions != null ? positions.size() : 0);
            syncLog.setTransactionsFetched(transactions != null ? transactions.size() : 0);
            syncLog.setTradesCreated(tradesCreated);
            syncLog.setTradesDuplicated(tradesDuplicated);
            syncLog.setStatus(OpenFinanceSyncStatus.SUCCESS);
            syncLogPort.save(syncLog);

            connection.setStatus(OpenFinanceConnectionStatus.ACTIVE);
            connection.setLastSyncAt(completedAt);
            connection.setLastSyncStatus(OpenFinanceSyncStatus.SUCCESS);
            connection.setErrorMessage(null);
            connection.setUpdatedAt(completedAt);
            connectionPort.save(connection);

            log.info("Sincronização Open Finance concluída com sucesso: connectionId={}, positions={}, transactions={}, created={}, duplicated={}",
                    connectionId, syncLog.getPositionsFetched(), syncLog.getTransactionsFetched(), tradesCreated, tradesDuplicated);

            return new OpenFinanceSyncResultDTO(
                    connection.getId(),
                    OpenFinanceSyncStatus.SUCCESS,
                    syncLog.getPositionsFetched(),
                    syncLog.getTransactionsFetched(),
                    tradesCreated,
                    tradesDuplicated,
                    "Sincronização realizada com sucesso.",
                    completedAt
            );
        } catch (Exception e) {
            log.error("Erro durante a execução do pipeline de sincronização Open Finance: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);

            LocalDateTime errorAt = LocalDateTime.now(ZoneOffset.UTC);
            syncLog.setCompletedAt(errorAt);
            syncLog.setStatus(OpenFinanceSyncStatus.FAILED);
            syncLog.setErrorDetails(e.getMessage());
            syncLogPort.save(syncLog);

            boolean isConsentExpired = connection.getConsentExpiresAt() != null
                    && connection.getConsentExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC));
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase(Locale.ROOT) : "";
            boolean isConsentError = errorMsg.contains("consent") || errorMsg.contains("expir") || errorMsg.contains("revog");

            OpenFinanceConnectionStatus nextStatus = (isConsentExpired || isConsentError)
                    ? OpenFinanceConnectionStatus.EXPIRED
                    : OpenFinanceConnectionStatus.FAILED;

            connection.setStatus(nextStatus);
            connection.setLastSyncAt(errorAt);
            connection.setLastSyncStatus(OpenFinanceSyncStatus.FAILED);
            connection.setErrorMessage(e.getMessage());
            connection.setUpdatedAt(errorAt);
            connectionPort.save(connection);

            throw new RuntimeException("Falha na sincronização Open Finance: " + e.getMessage(), e);
        }
    }

    private String normalizeTradeSide(String side) {
        if (side == null || side.isBlank()) {
            return null;
        }
        String upper = side.toUpperCase(Locale.ROOT).trim();
        if (upper.contains("COMPRA") || upper.contains("BUY")) {
            return "COMPRA";
        }
        if (upper.contains("VENDA") || upper.contains("SELL")) {
            return "VENDA";
        }
        return null;
    }
}
