package afsdigital.grahamselect.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.valuation.application.usecase.CalculationIntrinsicValueUseCase;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.InvalidFinancialDataEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialDataConsumerServiceTest {

    @Mock
    private CalculationIntrinsicValueUseCase calculationIntrinsicValueUseCase;

    @InjectMocks
    private FinancialDataConsumerService consumerService;

    private FinancialDataKey key;
    private FinancialDataEvent event;

    @BeforeEach
    void setUp() {
        key = new FinancialDataKey("PETR4", LocalDate.of(2024, 12, 31));
        event = FinancialDataEvent.builder()
                .ticker("PETR4")
                .resultDate(LocalDate.of(2024, 12, 31))
                .price(35.50)
                .earningsPerShare(2.50)
                .bookValuePerShare(15.00)
                .build();
    }

    @Test
    void shouldConsumeAndProcessFinancialDataEventSuccessfully() {
        // Given
        ConsumerRecord<FinancialDataKey, FinancialDataEvent> record = new ConsumerRecord<>("FINANCIAL_DATA_TOPIC", 0,
                0L, key, event);

        // When
        consumerService.consume(record);

        // Then
        verify(calculationIntrinsicValueUseCase, times(1)).process(event);
    }

    @Test
    void shouldLogWarningWhenEventIsNull() {
        // Given
        ConsumerRecord<FinancialDataKey, FinancialDataEvent> record = new ConsumerRecord<>("FINANCIAL_DATA_TOPIC", 0,
                0L, key, null);

        // When
        consumerService.consume(record);

        // Then
        verify(calculationIntrinsicValueUseCase, never()).process(any());
    }

    @Test
    void shouldLogErrorWhenProcessingFails() {
        // Given
        ConsumerRecord<FinancialDataKey, FinancialDataEvent> record = new ConsumerRecord<>("FINANCIAL_DATA_TOPIC", 0,
                0L, key, event);

        doThrow(new InvalidFinancialDataEventException("Invalid ticker"))
                .when(calculationIntrinsicValueUseCase).process(event);

        // When
        consumerService.consume(record);

        // Then
        verify(calculationIntrinsicValueUseCase, times(1)).process(event);
        // Exception should be caught and logged, not propagated
    }

    @Test
    void shouldHandleNullKeyGracefully() {
        // Given
        ConsumerRecord<FinancialDataKey, FinancialDataEvent> record = new ConsumerRecord<>("FINANCIAL_DATA_TOPIC", 0,
                0L, null, event);

        // When
        consumerService.consume(record);

        // Then
        verify(calculationIntrinsicValueUseCase, times(1)).process(event);
    }

    @Test
    void shouldHandleRuntimeExceptionGracefully() {
        // Given
        ConsumerRecord<FinancialDataKey, FinancialDataEvent> record = new ConsumerRecord<>("FINANCIAL_DATA_TOPIC", 0,
                0L, key, event);

        doThrow(new RuntimeException("Database connection error"))
                .when(calculationIntrinsicValueUseCase).process(event);

        // When
        consumerService.consume(record);

        // Then
        verify(calculationIntrinsicValueUseCase, times(1)).process(event);
        // Exception should be caught and logged, not propagated
    }
}
