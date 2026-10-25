package afsdigital.grahamselect.api.dlq.infrastructure.messaging;

import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.entities.DlqMessageEntity;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.repositories.DlqMessageRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractionFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaTradeExtractionDlqConsumerTest {

    @Mock
    private DlqMessageRepository dlqMessageRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private KafkaTradeExtractionDlqConsumer consumer;

    private TradeExtractionFailedEvent event;
    private String payloadJson;

    @BeforeEach
    void setUp() throws Exception {
        event = TradeExtractionFailedEvent.builder()
                .eventId("event-12345")
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId("user-abc-123")
                .correlationId("corr-789")
                .fileName("extrato_b3.xlsx")
                .lineNumber(15L)
                .originalPayload(Map.of("ticker", "PETR4", "quantidade", "100", "preco", "35.50", "data", "10/05/2023"))
                .reason("Formato de data inválido na coluna D")
                .build();

        payloadJson = objectMapper.writeValueAsString(event);
    }

    @Test
    @DisplayName("Deve consumir mensagem do tópico DLQ e persistir na tabela dlq_messages com status PENDING")
    void shouldConsumeAndPersistDlqMessageWhenNotAlreadyExists() {
        when(dlqMessageRepository.findByEventId("event-12345")).thenReturn(Optional.empty());
        when(dlqMessageRepository.save(any(DlqMessageEntity.class))).thenAnswer(inv -> {
            DlqMessageEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        consumer.consumeTradeExtractionDlq(payloadJson);

        ArgumentCaptor<DlqMessageEntity> captor = ArgumentCaptor.forClass(DlqMessageEntity.class);
        verify(dlqMessageRepository).save(captor.capture());

        DlqMessageEntity saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo("event-12345");
        assertThat(saved.getCorrelationId()).isEqualTo("corr-789");
        assertThat(saved.getUserId()).isEqualTo("user-abc-123");
        assertThat(saved.getFileName()).isEqualTo("extrato_b3.xlsx");
        assertThat(saved.getLineNumber()).isEqualTo(15L);
        assertThat(saved.getErrorMessage()).isEqualTo("Formato de data inválido na coluna D");
        assertThat(saved.getOriginalTopic()).isEqualTo(TopicConstants.TRADE_EXTRACTED_TOPIC);
        assertThat(saved.getStatus()).isEqualTo(DlqMessageStatus.PENDING);
        assertThat(saved.getOriginalPayload()).contains("PETR4");
    }

    @Test
    @DisplayName("Deve garantir idempotência e ignorar mensagem caso eventId já exista no banco")
    void shouldIgnoreMessageWhenEventIdAlreadyExists() {
        DlqMessageEntity existing = DlqMessageEntity.builder()
                .id(99L)
                .eventId("event-12345")
                .status(DlqMessageStatus.PENDING)
                .build();
        when(dlqMessageRepository.findByEventId("event-12345")).thenReturn(Optional.of(existing));

        consumer.consumeTradeExtractionDlq(payloadJson);

        verify(dlqMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve capturar exceção e não quebrar caso o payload seja inválido")
    void shouldCatchExceptionAndNotThrowWhenPayloadIsMalformed() {
        consumer.consumeTradeExtractionDlq("{ invalid json ");

        verify(dlqMessageRepository, never()).save(any());
    }
}
