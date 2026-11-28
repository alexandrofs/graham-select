package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.events.ValuationRequestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

final class ManualTradeUseCaseSupport {

    private ManualTradeUseCaseSupport() {
    }

    static String serializeTradeForAudit(ObjectMapper objectMapper, Trade trade, String actionType, Logger log) {
        try {
            return objectMapper.writeValueAsString(trade);
        } catch (JsonProcessingException e) {
            log.error("Falha ao serializar trade para auditoria ({}): tradeId={}, userId={}",
                    actionType, trade != null ? trade.getId() : null, trade != null ? trade.getUserId() : null, e);
            throw new IllegalStateException("Falha ao serializar trade para auditoria", e);
        }
    }

    static void publishValuationAfterCommit(ValuationEventPort valuationEventPort, String userId, String ticker) {
        ValuationRequestedEvent event = ValuationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .ticker(ticker)
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    valuationEventPort.publishValuationRequest(event);
                }
            });
        } else {
            valuationEventPort.publishValuationRequest(event);
        }
    }
}
