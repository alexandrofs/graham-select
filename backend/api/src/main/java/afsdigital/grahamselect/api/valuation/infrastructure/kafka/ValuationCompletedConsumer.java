package afsdigital.grahamselect.api.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.domain.entities.ValuationCompletedEvent;
import afsdigital.grahamselect.common.domain.entities.ValuationFailedEvent;
import afsdigital.grahamselect.common.portfolio.application.repository.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationCompletedConsumer {

    private final NotificationPort notificationPort;

    @KafkaListener(topics = TopicConstants.VALUATION_COMPLETED_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeValuationCompleted(ConsumerRecord<String, Object> record) {
        log.info("Received message on topic: {}", TopicConstants.VALUATION_COMPLETED_TOPIC);
        
        if (!(record.value() instanceof ValuationCompletedEvent event)) {
            log.error("Invalid event type received on {}: {}", TopicConstants.VALUATION_COMPLETED_TOPIC, 
                    record.value() != null ? record.value().getClass().getName() : "null");
            return;
        }

        String userId = event.userId();
        if (userId == null || userId.isBlank()) {
            log.error("Consumed valuation-completed event with null or empty userId");
            return;
        }

        log.info("Consumed valuation-completed event for userId: {}, recommendations: {}, excluded: {}", 
                userId, event.recommendationCount(), event.excludedTickers() != null ? event.excludedTickers().size() : 0);

        notificationPort.sendNotification(userId, "VALUATION_COMPLETED", event);
    }

    @KafkaListener(topics = TopicConstants.VALUATION_FAILED_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeValuationFailed(ConsumerRecord<String, Object> record) {
        log.info("Received message on topic: {}", TopicConstants.VALUATION_FAILED_TOPIC);

        if (!(record.value() instanceof ValuationFailedEvent event)) {
            log.error("Invalid event type received on {}: {}", TopicConstants.VALUATION_FAILED_TOPIC,
                    record.value() != null ? record.value().getClass().getName() : "null");
            return;
        }

        String userId = event.userId();
        if (userId == null || userId.isBlank()) {
            log.error("Consumed valuation-failed event with null or empty userId");
            return;
        }

        log.error("Consumed valuation-failed event for userId: {}, error: {}", userId, event.message());

        notificationPort.sendNotification(userId, "VALUATION_FAILED", event);
    }
}
