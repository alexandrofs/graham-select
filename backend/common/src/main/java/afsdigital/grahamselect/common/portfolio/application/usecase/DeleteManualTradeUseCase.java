package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Deletes a trade owned by the user, recording an audit entry and requesting valuation.
 */
@Slf4j
@RequiredArgsConstructor
public class DeleteManualTradeUseCase {

    private final TradePort tradePort;
    private final ManualTradeAuditPort auditPort;
    private final ValuationEventPort valuationEventPort;
    private final ObjectMapper objectMapper;

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
                .payloadJson(ManualTradeUseCaseSupport.serializeTradeForAudit(objectMapper, existing, "DELETE", log))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        auditPort.save(audit);

        tradePort.deleteByIdAndUserId(tradeId, userId);

        ManualTradeUseCaseSupport.publishValuationAfterCommit(valuationEventPort, userId, existing.getTicker());

        return true;
    }
}
