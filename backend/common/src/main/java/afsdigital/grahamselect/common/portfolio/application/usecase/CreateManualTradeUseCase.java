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

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CreateManualTradeUseCase {
    private final TradePort tradePort;
    private final ManualTradeAuditPort auditPort;
    private final ValuationEventPort valuationEventPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public Trade execute(Trade trade) {
        boolean alreadyExists = tradePort.exists(
                trade.getUserId(),
                trade.getProfileId(),
                trade.getTicker(),
                trade.getTradeDate(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getBroker(),
                trade.getSide()
        );

        if (alreadyExists) {
            return null;
        }

        // 1. Persist Trade
        Trade savedTrade = tradePort.save(trade);

        // 2. Create Audit Record
        ManualTradeAudit audit = ManualTradeAudit.builder()
                .id(UUID.randomUUID())
                .tradeId(savedTrade.getId())
                .userId(savedTrade.getUserId())
                .actionType("CREATE")
                .payloadJson(ManualTradeUseCaseSupport.serializeTradeForAudit(objectMapper, savedTrade, "CREATE", log))
                .createdAt(savedTrade.getCreatedAt())
                .build();
        auditPort.save(audit);

        // 3. Request Valuation after commit
        ManualTradeUseCaseSupport.publishValuationAfterCommit(
                valuationEventPort, savedTrade.getUserId(), savedTrade.getTicker());

        return savedTrade;
    }
}
