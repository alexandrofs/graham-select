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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RequiredArgsConstructor
public class CreateManualTradeUseCase {
    private final TradePort tradePort;
    private final ManualTradeAuditPort auditPort;
    private final ValuationEventPort valuationEventPort;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Transactional
    public Trade execute(Trade trade) {
        boolean alreadyExists = tradePort.exists(
                trade.getUserId(),
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
                .payloadJson(objectMapper.writeValueAsString(savedTrade))
                .createdAt(savedTrade.getCreatedAt())
                .build();
        auditPort.save(audit);

        // 3. Request Valuation
        ValuationRequestedEvent event = ValuationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(savedTrade.getUserId())
                .ticker(savedTrade.getTicker())
                .build();
        valuationEventPort.publishValuationRequest(event);

        return savedTrade;
    }
}
