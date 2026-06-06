package afsdigital.grahamselect.api.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaMarketDataEventPublisherTest {

    @Mock
    private KafkaTemplate<FinancialDataKey, FinancialDataEvent> kafkaTemplate;

    @InjectMocks
    private KafkaMarketDataEventPublisher kafkaMarketDataEventPublisher;

    @Test
    void shouldPublishEventSuccessfully() throws Exception {
        // Arrange
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        FinancialDataEvent event = FinancialDataEvent.builder()
                .ticker("PETR4")
                .price(35.50)
                .dividendYield(8.5)
                .priceEarnings(5.2)
                .priceToBook(1.2)
                .earningsPerShare(6.8)
                .bookValuePerShare(29.5)
                .resultDate(today)
                .build();

        SendResult<FinancialDataKey, FinancialDataEvent> sendResult = mock(SendResult.class);
        TopicPartition topicPartition = new TopicPartition(TopicConstants.FINANCIAL_DATA_TOPIC, 0);
        RecordMetadata recordMetadata = new RecordMetadata(topicPartition, 42L, 0, 0L, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        when(kafkaTemplate.send(anyString(), any(FinancialDataKey.class), any(FinancialDataEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // Act
        assertDoesNotThrow(() -> kafkaMarketDataEventPublisher.publish(event));

        // Assert
        ArgumentCaptor<FinancialDataKey> keyCaptor = ArgumentCaptor.forClass(FinancialDataKey.class);
        ArgumentCaptor<FinancialDataEvent> eventCaptor = ArgumentCaptor.forClass(FinancialDataEvent.class);

        verify(kafkaTemplate, times(1)).send(
                eq(TopicConstants.FINANCIAL_DATA_TOPIC),
                keyCaptor.capture(),
                eventCaptor.capture()
        );

        FinancialDataKey capturedKey = keyCaptor.getValue();
        assertEquals("PETR4", capturedKey.getTicker());
        assertEquals(today, capturedKey.getReferenceDate());

        FinancialDataEvent capturedEvent = eventCaptor.getValue();
        assertEquals("PETR4", capturedEvent.getTicker());
        assertEquals(35.50, capturedEvent.getPrice());
        assertEquals(today, capturedEvent.getResultDate());
    }

    @Test
    void shouldThrowExceptionWhenKafkaFails() {
        // Arrange
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        FinancialDataEvent event = FinancialDataEvent.builder()
                .ticker("PETR4")
                .resultDate(today)
                .build();

        CompletableFuture<SendResult<FinancialDataKey, FinancialDataEvent>> failingFuture = new CompletableFuture<>();
        failingFuture.completeExceptionally(new RuntimeException("Kafka timeout error"));

        when(kafkaTemplate.send(anyString(), any(FinancialDataKey.class), any(FinancialDataEvent.class)))
                .thenReturn(failingFuture);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> kafkaMarketDataEventPublisher.publish(event));
        assertTrue(exception.getMessage().contains("Failed to publish market data event to Kafka"));
        verify(kafkaTemplate, times(1)).send(eq(TopicConstants.FINANCIAL_DATA_TOPIC), any(FinancialDataKey.class), any(FinancialDataEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> kafkaMarketDataEventPublisher.publish(null));
    }

    @Test
    void shouldThrowExceptionWhenTickerIsNull() {
        // Arrange
        FinancialDataEvent event = FinancialDataEvent.builder()
                .resultDate(LocalDate.now(ZoneOffset.UTC))
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> kafkaMarketDataEventPublisher.publish(event));
    }

    @Test
    void shouldThrowExceptionWhenResultDateIsNull() {
        // Arrange
        FinancialDataEvent event = FinancialDataEvent.builder()
                .ticker("PETR4")
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> kafkaMarketDataEventPublisher.publish(event));
    }
}
