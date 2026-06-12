package afsdigital.grahamselect.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.domain.entities.ValuationCompletedEvent;
import afsdigital.grahamselect.common.domain.entities.ValuationRequestedEvent;
import afsdigital.grahamselect.valuation.application.usecase.GenerateGrahamRecommendationsUseCase;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ValuationRequestedConsumerService {

    private final GenerateGrahamRecommendationsUseCase generateGrahamRecommendationsUseCase;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = TopicConstants.VALUATION_REQUESTED_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, ValuationRequestedEvent> record) {
        ValuationRequestedEvent event = record.value();
        if (event == null) {
            log.warn("Received null valuation requested event");
            return;
        }

        String userId = event.userId();
        if (userId == null || userId.isBlank()) {
            log.error("Received valuation requested event with null or empty userId");
            return;
        }

        log.info("Consumed valuation-requested event for userId: {}", userId);

        try {
            List<GrahamRecommendation> recommendations = generateGrahamRecommendationsUseCase.execute(userId);

            ValuationCompletedEvent completedEvent = new ValuationCompletedEvent(
                    userId,
                    recommendations.size(),
                    OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    UUID.randomUUID()
            );

            kafkaTemplate.send(TopicConstants.VALUATION_COMPLETED_TOPIC, userId, completedEvent);
            log.info("Successfully processed valuation request and published completion for userId: {}", userId);

        } catch (Exception e) {
            log.error("Error processing valuation-requested for userId: {}. Error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to process valuation requested event", e);
        }
    }
}
