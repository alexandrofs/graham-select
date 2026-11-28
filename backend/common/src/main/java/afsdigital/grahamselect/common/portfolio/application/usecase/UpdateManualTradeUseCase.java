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
 * Updates an existing trade owned by the user, recording an audit entry and requesting valuation.
 */
@RequiredArgsConstructor
public class UpdateManualTradeUseCase {

    public enum UpdateStatus {
        UPDATED,
        NOT_FOUND,
        CONFLICT
    }

    public record UpdateResult(UpdateStatus status, Trade trade) {
    }

    private final TradePort tradePort;
    private final ManualTradeAuditPort auditPort;
    private final ValuationEventPort valuationEventPort;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Transactional
    public UpdateResult execute(UUID tradeId, String userId, Trade updatedFields) {
        Optional<Trade> existingOpt = tradePort.findByIdAndUserId(tradeId, userId);
        if (existingOpt.isEmpty()) {
            return new UpdateResult(UpdateStatus.NOT_FOUND, null);
        }

        Trade existing = existingOpt.get();
        boolean duplicate = tradePort.existsOtherWithSameSignature(
                tradeId,
                userId,
                updatedFields.getProfileId(),
                updatedFields.getTicker(),
                updatedFields.getTradeDate(),
                updatedFields.getQuantity(),
                updatedFields.getPrice(),
                updatedFields.getBroker(),
                updatedFields.getSide()
        );
        if (duplicate) {
            return new UpdateResult(UpdateStatus.CONFLICT, null);
        }

        Trade toSave = Trade.builder()
                .id(existing.getId())
                .userId(existing.getUserId())
                .profileId(updatedFields.getProfileId())
                .ticker(updatedFields.getTicker())
                .side(updatedFields.getSide())
                .tradeDate(updatedFields.getTradeDate())
                .quantity(updatedFields.getQuantity())
                .price(updatedFields.getPrice())
                .broker(updatedFields.getBroker())
                .assetClass(updatedFields.getAssetClass())
                .createdAt(existing.getCreatedAt())
                .build();

        Trade savedTrade = tradePort.save(toSave);

        ManualTradeAudit audit = ManualTradeAudit.builder()
                .id(UUID.randomUUID())
                .tradeId(savedTrade.getId())
                .userId(userId)
                .actionType("UPDATE")
                .payloadJson(objectMapper.writeValueAsString(savedTrade))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        auditPort.save(audit);

        publishValuation(userId, savedTrade.getTicker());
        if (existing.getTicker() != null && !existing.getTicker().equalsIgnoreCase(savedTrade.getTicker())) {
            publishValuation(userId, existing.getTicker());
        }

        return new UpdateResult(UpdateStatus.UPDATED, savedTrade);
    }

    private void publishValuation(String userId, String ticker) {
        ValuationRequestedEvent event = ValuationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .ticker(ticker)
                .build();
        valuationEventPort.publishValuationRequest(event);
    }
}
