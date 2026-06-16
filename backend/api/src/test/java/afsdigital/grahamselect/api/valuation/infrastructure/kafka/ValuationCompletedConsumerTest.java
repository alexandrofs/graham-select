package afsdigital.grahamselect.api.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.domain.entities.ValuationCompletedEvent;
import afsdigital.grahamselect.common.domain.entities.ValuationFailedEvent;
import afsdigital.grahamselect.common.portfolio.application.repository.NotificationPort;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ValuationCompletedConsumerTest {

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private ValuationCompletedConsumer valuationCompletedConsumer;

    @Test
    public void shouldNotifyValuationCompleted() {
        String userId = "user-123";
        ValuationCompletedEvent event = new ValuationCompletedEvent(
                userId, 10, List.of(), "2026-06-13T16:00:00Z", UUID.randomUUID()
        );
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(TopicConstants.VALUATION_COMPLETED_TOPIC, 0, 0, userId, event);

        valuationCompletedConsumer.consumeValuationCompleted(record);

        verify(notificationPort).sendNotification(eq(userId), eq("VALUATION_COMPLETED"), eq(event));
    }

    @Test
    public void shouldNotifyValuationFailed() {
        String userId = "user-123";
        ValuationFailedEvent event = new ValuationFailedEvent(
                userId, "ERROR", "Message", "2026-06-13T16:00:00Z", UUID.randomUUID()
        );
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(TopicConstants.VALUATION_FAILED_TOPIC, 0, 0, userId, event);

        valuationCompletedConsumer.consumeValuationFailed(record);

        verify(notificationPort).sendNotification(eq(userId), eq("VALUATION_FAILED"), eq(event));
    }
}
