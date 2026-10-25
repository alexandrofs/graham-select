package afsdigital.grahamselect.api.dlq.application.service;

import afsdigital.grahamselect.api.dlq.application.dto.DlqBatchReprocessResultDto;
import afsdigital.grahamselect.api.dlq.application.dto.DlqMessageDto;
import afsdigital.grahamselect.api.dlq.application.dto.DlqReprocessResultDto;
import afsdigital.grahamselect.api.dlq.application.service.exceptions.DlqMessageNotFoundException;
import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.entities.DlqMessageEntity;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.repositories.DlqMessageRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
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
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DlqManagementServiceTest {

    @Mock
    private DlqMessageRepository dlqMessageRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private DlqManagementService dlqManagementService;

    private DlqMessageEntity pendingEntity;

    @BeforeEach
    void setUp() {
        lenient().when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        pendingEntity = DlqMessageEntity.builder()
                .id(10L)
                .eventId("event-uuid-1")
                .correlationId("corr-123")
                .userId("user-plain-id")
                .fileName("extrato_outubro.xlsx")
                .lineNumber(22L)
                .errorMessage("Coluna PREÇO continha caractere R$")
                .originalPayload("{\"ticker\":\"VALE3\",\"quantidade\":\"200\",\"preco\":\"R$ 68,50\",\"data\":\"15/10/2023\",\"side\":\"COMPRA\",\"corretora\":\"XP\"}")
                .originalTopic(TopicConstants.TRADE_EXTRACTED_TOPIC)
                .status(DlqMessageStatus.PENDING)
                .createdAt(LocalDateTime.of(2023, 10, 15, 14, 30, 0))
                .updatedAt(LocalDateTime.of(2023, 10, 15, 14, 30, 0))
                .build();
    }

    @Test
    @DisplayName("Deve listar falhas pendentes com ordenação decrescente e mascarar userId no formato usr_***xxxx")
    void shouldReturnPendingFailuresWithAnonymizedUserId() {
        when(dlqMessageRepository.findByStatusOrderByCreatedAtDesc(DlqMessageStatus.PENDING))
                .thenReturn(List.of(pendingEntity));

        List<DlqMessageDto> result = dlqManagementService.getPendingFailures(DlqMessageStatus.PENDING);

        assertThat(result).hasSize(1);
        DlqMessageDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.anonymizedUserId()).startsWith("usr_***");
        assertThat(dto.anonymizedUserId()).isNotEqualTo("user-plain-id");
        assertThat(dto.fileName()).isEqualTo("extrato_outubro.xlsx");
        assertThat(dto.errorMessage()).isEqualTo("Coluna PREÇO continha caractere R$");
        assertThat(dto.status()).isEqualTo(DlqMessageStatus.PENDING);
        assertThat(dto.timestamp()).isEqualTo(LocalDateTime.of(2023, 10, 15, 14, 30, 0).atOffset(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Deve reprocessar com sucesso, gerar eventId UUID válido, publicar no Kafka e marcar como RESOLVED")
    void shouldReprocessSuccessfullyAndPublishToKafka() {
        when(dlqMessageRepository.findById(10L)).thenReturn(Optional.of(pendingEntity));
        when(dlqMessageRepository.save(any(DlqMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DlqReprocessResultDto result = dlqManagementService.reprocess(10L);

        assertThat(result.status()).isEqualTo(DlqMessageStatus.RESOLVED);
        assertThat(result.message()).contains("sucesso");

        ArgumentCaptor<TradeExtractedEvent> eventCaptor = ArgumentCaptor.forClass(TradeExtractedEvent.class);
        verify(kafkaTemplate).send(
                eq(TopicConstants.TRADE_EXTRACTED_TOPIC),
                eq("user-plain-id:VALE3"),
                eventCaptor.capture()
        );

        TradeExtractedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.ticker()).isEqualTo("VALE3");
        assertThat(publishedEvent.tradeDate()).isEqualTo(LocalDate.of(2023, 10, 15));
        assertThat(publishedEvent.quantity()).isEqualByComparingTo(new BigDecimal("200.0000"));
        assertThat(publishedEvent.price()).isEqualByComparingTo(new BigDecimal("68.5000"));
        assertThat(publishedEvent.side()).isEqualTo("COMPRA");
        assertThat(publishedEvent.broker()).isEqualTo("XP");

        // UUID deve ser válido para evitar quebra no consumidor downstream UUID.fromString
        assertThat(publishedEvent.eventId()).isNotNull();
        UUID parsedUuid = UUID.fromString(publishedEvent.eventId());
        assertThat(parsedUuid).isNotNull();

        assertThat(pendingEntity.getStatus()).isEqualTo(DlqMessageStatus.RESOLVED);
        assertThat(pendingEntity.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve reprocessar com formato de data ISO yyyy-MM-dd resiliente")
    void shouldReprocessWithIsoDateFormat() {
        pendingEntity.setOriginalPayload("{\"ticker\":\"PETR4\",\"quantidade\":\"100\",\"preco\":\"30.00\",\"data\":\"2023-11-20\"}");
        when(dlqMessageRepository.findById(10L)).thenReturn(Optional.of(pendingEntity));
        when(dlqMessageRepository.save(any(DlqMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DlqReprocessResultDto result = dlqManagementService.reprocess(10L);

        assertThat(result.status()).isEqualTo(DlqMessageStatus.RESOLVED);
        ArgumentCaptor<TradeExtractedEvent> eventCaptor = ArgumentCaptor.forClass(TradeExtractedEvent.class);
        verify(kafkaTemplate).send(any(), any(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().tradeDate()).isEqualTo(LocalDate.of(2023, 11, 20));
    }

    @Test
    @DisplayName("Deve ser idempotente e não publicar no Kafka caso a mensagem já esteja RESOLVED")
    void shouldBeIdempotentWhenAlreadyResolved() {
        pendingEntity.setStatus(DlqMessageStatus.RESOLVED);
        when(dlqMessageRepository.findById(10L)).thenReturn(Optional.of(pendingEntity));

        DlqReprocessResultDto result = dlqManagementService.reprocess(10L);

        assertThat(result.status()).isEqualTo(DlqMessageStatus.RESOLVED);
        assertThat(result.message()).contains("já resolvida");
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar DlqMessageNotFoundException quando id não existir")
    void shouldThrowNotFoundExceptionWhenIdDoesNotExist() {
        when(dlqMessageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dlqManagementService.reprocess(999L))
                .isInstanceOf(DlqMessageNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Deve marcar como FAILED quando o payload for irrecuperável e preservar o erro original")
    void shouldMarkAsFailedWhenPayloadIsInvalid() {
        pendingEntity.setErrorMessage("Erro de parsing original no arquivo");
        pendingEntity.setOriginalPayload("{\"ticker\":\"\",\"quantidade\":\"0\",\"preco\":\"invalido\"}");
        when(dlqMessageRepository.findById(10L)).thenReturn(Optional.of(pendingEntity));
        when(dlqMessageRepository.save(any(DlqMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DlqReprocessResultDto result = dlqManagementService.reprocess(10L);

        assertThat(result.status()).isEqualTo(DlqMessageStatus.FAILED);
        assertThat(pendingEntity.getStatus()).isEqualTo(DlqMessageStatus.FAILED);
        assertThat(pendingEntity.getErrorMessage()).contains("Falha de reprocessamento");
        assertThat(pendingEntity.getErrorMessage()).contains("Erro de parsing original no arquivo");
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Deve suportar formato numérico americano e normalizar side V para VENDA")
    void shouldSupportUsNumberFormatAndNormalizeVendaSide() {
        pendingEntity.setOriginalPayload("{\"ticker\":\"ITSA4\",\"quantidade\":\"1,500.00\",\"preco\":\"12.75\",\"data\":\"2024-01-10\",\"side\":\"V\"}");
        when(dlqMessageRepository.findById(10L)).thenReturn(Optional.of(pendingEntity));
        when(dlqMessageRepository.save(any(DlqMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DlqReprocessResultDto result = dlqManagementService.reprocess(10L);

        assertThat(result.status()).isEqualTo(DlqMessageStatus.RESOLVED);
        ArgumentCaptor<TradeExtractedEvent> eventCaptor = ArgumentCaptor.forClass(TradeExtractedEvent.class);
        verify(kafkaTemplate).send(any(), any(), eventCaptor.capture());

        TradeExtractedEvent event = eventCaptor.getValue();
        assertThat(event.quantity()).isEqualByComparingTo(new BigDecimal("1500.0000"));
        assertThat(event.price()).isEqualByComparingTo(new BigDecimal("12.7500"));
        assertThat(event.side()).isEqualTo("VENDA");
    }

    @Test
    @DisplayName("Deve reprocessar em lote com isolamento de falhas e contabilizar sucessos e falhas")
    void shouldReprocessBatchWithAggregatedCounts() {
        DlqMessageEntity validEntity = pendingEntity;

        DlqMessageEntity invalidEntity = DlqMessageEntity.builder()
                .id(20L)
                .eventId("event-uuid-2")
                .correlationId("corr-456")
                .userId("user-2")
                .fileName("extrato_corrompido.xlsx")
                .originalPayload("{\"ticker\":\"\"}") // Falhará no ticker vazio
                .status(DlqMessageStatus.PENDING)
                .build();

        when(dlqMessageRepository.findById(10L)).thenReturn(Optional.of(validEntity));
        when(dlqMessageRepository.findById(20L)).thenReturn(Optional.of(invalidEntity));
        when(dlqMessageRepository.save(any(DlqMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DlqBatchReprocessResultDto batchResult = dlqManagementService.reprocessBatch(List.of(10L, 20L));

        assertThat(batchResult.totalProcessed()).isEqualTo(2);
        assertThat(batchResult.successCount()).isEqualTo(1);
        assertThat(batchResult.failureCount()).isEqualTo(1);
        assertThat(batchResult.details()).hasSize(2);
    }
}
