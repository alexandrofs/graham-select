package afsdigital.grahamselect.api.upload.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaFinancialEventDataProducerTest {

    @Mock
    private KafkaTemplate<FinancialDataKey, FinancialDataEvent> kafkaTemplate;

    @InjectMocks
    private KafkaFinancialEventDataProducer financialEventDataProducer;

    @Test
    void send_WhenFinancialDataEventIsValid_ShouldSendEventToKafka() {

        FinancialDataEvent financialDataEvent = FinancialDataEvent.builder().ticker("AAPL").resultDate(LocalDate.now()).build();
        FinancialDataKey key = new FinancialDataKey("AAPL", LocalDate.now());
        financialEventDataProducer.send(financialDataEvent);
        verify(kafkaTemplate, times(1)).send(TopicConstants.FINANCIAL_DATA_TOPIC, key, financialDataEvent);

    }

    @Test
    void send_WhenFinancialDataEventIsNull_ShouldThrowException() {

        assertThrows(IllegalArgumentException.class, () -> financialEventDataProducer.send(null));

    }

    @Test
    void send_WhenTickerIsNull_ShouldThrowException() {

        FinancialDataEvent financialDataEvent = FinancialDataEvent.builder().resultDate(LocalDate.now()).build();
        assertThrows(IllegalArgumentException.class, () -> financialEventDataProducer.send(financialDataEvent));

    }

    @Test
    void send_WhenResultDateIsNull_ShouldThrowException() {

        FinancialDataEvent financialDataEvent = FinancialDataEvent.builder().ticker("AAPL").build();
        assertThrows(IllegalArgumentException.class, () -> financialEventDataProducer.send(financialDataEvent));

    }
}

