package afsdigital.grahamselect.api.portfolio.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.domain.events.ValuationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaValuationEventPublisher implements ValuationEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishValuationRequest(ValuationRequestedEvent event) {
        kafkaTemplate.send(
                TopicConstants.VALUATION_REQUESTED_TOPIC,
                event.userId() + ":" + event.ticker(),
                event
        );
    }
}
