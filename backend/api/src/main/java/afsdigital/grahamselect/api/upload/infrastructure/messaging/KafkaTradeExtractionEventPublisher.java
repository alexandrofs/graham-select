package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.application.repository.TradeExtractionEventPort;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractionFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTradeExtractionEventPublisher implements TradeExtractionEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishExtractedTrade(TradeExtractedEvent event) {
        kafkaTemplate.send(
                TopicConstants.TRADE_EXTRACTED_TOPIC,
                event.userId() + ":" + event.ticker(),
                event
        );
    }

    @Override
    public void publishExtractionFailure(TradeExtractionFailedEvent event) {
        kafkaTemplate.send(
                TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC,
                event.correlationId(),
                event
        );
    }
}
