package afsdigital.grahamselect.api.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.domain.entities.ValuationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValuationRequestedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(ValuationRequestedEvent event) {
        log.info("Publishing valuation-requested event for userId: {}", event.userId());
        kafkaTemplate.send(TopicConstants.VALUATION_REQUESTED_TOPIC, event.userId(), event);
    }
}
