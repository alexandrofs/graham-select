package afsdigital.grahamselect.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.domain.entities.ValuationCompletedEvent;
import afsdigital.grahamselect.common.domain.entities.ValuationFailedEvent;
import afsdigital.grahamselect.common.domain.entities.ValuationRequestedEvent;
import afsdigital.grahamselect.valuation.application.dto.GenerationResult;
import afsdigital.grahamselect.valuation.application.usecase.GenerateGrahamRecommendationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

        UUID tagId = event.tagId();
        log.info("Consumed valuation-requested event for userId: {}, tagId: {}", userId, tagId);

        try {
            GenerationResult result = tagId == null
                    ? generateGrahamRecommendationsUseCase.execute(userId)
                    : generateGrahamRecommendationsUseCase.execute(userId, tagId);

            ValuationCompletedEvent completedEvent = new ValuationCompletedEvent(
                    userId,
                    result.recommendations().size(),
                    result.excludedTickers(),
                    OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    UUID.randomUUID(),
                    tagId
            );

            kafkaTemplate.send(TopicConstants.VALUATION_COMPLETED_TOPIC, userId, completedEvent);
            log.info("Successfully processed valuation request and published completion for userId: {}, tagId: {}", userId, tagId);

        } catch (Exception e) {
            log.error("[VALUATION] Fatal error processing valuation-requested for userId: {}, tagId: {}. Error: {}",
                    userId, tagId, e.getMessage(), e);

            ValuationFailedEvent failedEvent = new ValuationFailedEvent(
                    userId,
                    "VALUATION_CALCULATION_ERROR",
                    "Ocorreu um erro ao calcular o valuation. Por favor, tente novamente mais tarde.",
                    OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    UUID.randomUUID(),
                    tagId
            );

            kafkaTemplate.send(TopicConstants.VALUATION_FAILED_TOPIC, userId, failedEvent);
            throw new RuntimeException("Failed to process valuation requested event", e);
        }

    }
}
