package afsdigital.grahamselect.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.*;
import afsdigital.grahamselect.valuation.application.dto.GenerationResult;
import afsdigital.grahamselect.valuation.application.usecase.GenerateGrahamRecommendationsUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ValuationRequestedConsumerServiceTest {

    @Mock
    private GenerateGrahamRecommendationsUseCase generateGrahamRecommendationsUseCase;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ValuationRequestedConsumerService valuationRequestedConsumerService;

    @Test
    public void shouldPublishValuationCompletedWithExcludedTickers() {
        String userId = "user-123";
        ValuationRequestedEvent event = new ValuationRequestedEvent(userId, "2026-06-13T16:00:00Z", UUID.randomUUID());
        ConsumerRecord<String, ValuationRequestedEvent> record = new ConsumerRecord<>("topic", 0, 0, userId, event);

        GenerationResult result = new GenerationResult(
                List.of(), // no recommendations
                List.of(new ExcludedTicker("MGLU3", "STALE_DATA", "10 days old"))
        );

        when(generateGrahamRecommendationsUseCase.execute(userId)).thenReturn(result);

        valuationRequestedConsumerService.consume(record);

        verify(kafkaTemplate).send(eq(TopicConstants.VALUATION_COMPLETED_TOPIC), eq(userId), argThat(payload -> {
            ValuationCompletedEvent completedEvent = (ValuationCompletedEvent) payload;
            return completedEvent.userId().equals(userId) &&
                   completedEvent.excludedTickers().size() == 1 &&
                   completedEvent.excludedTickers().get(0).ticker().equals("MGLU3");
        }));
    }

    @Test
    public void shouldPublishValuationFailedOnException() {
        String userId = "user-123";
        ValuationRequestedEvent event = new ValuationRequestedEvent(userId, "2026-06-13T16:00:00Z", UUID.randomUUID());
        ConsumerRecord<String, ValuationRequestedEvent> record = new ConsumerRecord<>("topic", 0, 0, userId, event);

        when(generateGrahamRecommendationsUseCase.execute(userId)).thenThrow(new RuntimeException("Fatal error"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            valuationRequestedConsumerService.consume(record);
        });

        verify(kafkaTemplate).send(eq(TopicConstants.VALUATION_FAILED_TOPIC), eq(userId), argThat(payload -> {
            ValuationFailedEvent failedEvent = (ValuationFailedEvent) payload;
            return failedEvent.userId().equals(userId) &&
                   failedEvent.errorCode().equals("VALUATION_CALCULATION_ERROR") &&
                   failedEvent.message().contains("erro ao calcular o valuation");
        }));
    }
}
