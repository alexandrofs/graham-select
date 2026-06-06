package afsdigital.grahamselect.api.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.valuation.application.repository.MarketDataEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
public class KafkaMarketDataEventPublisher implements MarketDataEventPublisherPort {

    private final KafkaTemplate<FinancialDataKey, FinancialDataEvent> kafkaTemplate;

    @Override
    public void publish(FinancialDataEvent event) {
        if (event == null || event.getTicker() == null || event.getResultDate() == null) {
            throw new IllegalArgumentException("FinancialDataEvent, ticker, and resultDate must not be null");
        }

        log.info("Publishing market data event for ticker: {} at {}", event.getTicker(), OffsetDateTime.now(ZoneOffset.UTC));

        FinancialDataKey key = new FinancialDataKey(event.getTicker(), event.getResultDate());

        try {
            var result = kafkaTemplate.send(TopicConstants.FINANCIAL_DATA_TOPIC, key, event)
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
            log.info("Successfully published market data event for ticker: {} with offset: {}",
                    event.getTicker(), result.getRecordMetadata().offset());
        } catch (Exception ex) {
            log.error("Failed to publish market data event for ticker: {}. Error: {}",
                    event.getTicker(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to publish market data event to Kafka", ex);
        }
    }
}
