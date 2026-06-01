package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SendPortfolioUpdateNotificationUseCase;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaTradeExtractedConsumer {

    private final ObjectMapper objectMapper;
    private final SaveTradeUseCase saveTradeUseCase;
    private final B3ImportStatusPort importStatusPort;
    private final SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase;

    @KafkaListener(topics = TopicConstants.TRADE_EXTRACTED_TOPIC, groupId = "api-group")
    public void consume(String payload) throws JsonProcessingException {
        log.debug("Consuming trade extracted event: {}", payload);
        TradeExtractedEvent event = objectMapper.readValue(payload, TradeExtractedEvent.class);

        Trade trade = Trade.builder()
                .id(UUID.fromString(event.eventId()))
                .userId(event.userId())
                .ticker(event.ticker())
                .tradeDate(event.tradeDate())
                .side(event.side() != null ? event.side() : "COMPRA")
                .quantity(event.quantity())
                .price(event.price())
                .broker(event.broker())
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        try {
            Trade savedTrade = saveTradeUseCase.execute(trade);

            if (savedTrade == null) {
                log.info("Duplicated trade ignored (logical check): {} for user {} at {}", event.ticker(), event.userId(), event.tradeDate());
                importStatusPort.addDuplicate(event.correlationId(), event.userId(), 1);
            } else {
                log.info("New trade saved: {} for user {} at {}", event.ticker(), event.userId(), event.tradeDate());
                sendPortfolioUpdateNotificationUseCase.execute(event.userId());
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicated trade ignored (DB constraint): {} for user {} at {}. Race condition detected but handled.", 
                    event.ticker(), event.userId(), event.tradeDate());
            importStatusPort.addDuplicate(event.correlationId(), event.userId(), 1);
        } catch (Exception e) {
            log.error("Error processing trade extracted event: {}", e.getMessage(), e);
            importStatusPort.addProgress(event.correlationId(), event.userId(), 0, 1, e.getMessage());
        }
    }
}
