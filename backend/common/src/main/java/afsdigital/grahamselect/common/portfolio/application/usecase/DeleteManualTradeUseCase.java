package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.events.ValuationRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Deletes a trade owned by the user, recording an audit entry and requesting valuation.
 */
@RequiredArgsConstructor
public class DeleteManualTradeUseCase {

    private final TradePort tradePort;
    private final ManualTradeAuditPort auditPort;
    private final ValuationEventPort valuationEventPort;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Transactional
    public boolean execute(UUID tradeId, String userId) {
        Optional<Trade> existingOpt = tradePort.findByIdAndUserId(tradeId, userId);
        if (existingOpt.isEmpty()) {
            return false;
        }

        Trade existing = existingOpt.get();
        ManualTradeAudit audit = ManualTradeAudit.builder()
                .id(UUID.randomUUID())
                .tradeId(existing.getId())
                .userId(userId)
                .actionType("DELETE")
                .payloadJson(objectMapper.writeValueAsString(existing))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        auditPort.save(audit);

        tradePort.deleteByIdAndUserId(tradeId, userId);

        ValuationRequestedEvent event = ValuationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .ticker(existing.getTicker())
                .build();
        valuationEventPort.publishValuationRequest(event);

        return true;
    }
}
