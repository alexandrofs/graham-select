package afsdigital.grahamselect.api.dlq.infrastructure.messaging;

import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.entities.DlqMessageEntity;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.repositories.DlqMessageRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractionFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaTradeExtractionDlqConsumer {

    private final DlqMessageRepository dlqMessageRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:api-dlq-consumer-group}"
    )
    public void consumeTradeExtractionDlq(String payload) {
        log.info("Recebida mensagem no tópico DLQ {}", TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC);
        try {
            TradeExtractionFailedEvent event = objectMapper.readValue(payload, TradeExtractionFailedEvent.class);

            if (event.eventId() != null && !event.eventId().isBlank() && dlqMessageRepository.findByEventId(event.eventId()).isPresent()) {
                log.warn("Mensagem DLQ já persistida para o eventId: {}. Ignorando.", event.eventId());
                return;
            }

            String originalPayloadJson = null;
            if (event.originalPayload() != null) {
                originalPayloadJson = objectMapper.writeValueAsString(event.originalPayload());
            }

            String errorMessage = event.reason() != null ? event.reason() : "Erro desconhecido na extração";
            if (errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000);
            }

            String safeEventId = (event.eventId() != null && !event.eventId().isBlank()) ? event.eventId() : UUID.randomUUID().toString();
            String safeCorrelationId = (event.correlationId() != null && !event.correlationId().isBlank()) ? event.correlationId() : UUID.randomUUID().toString();
            String safeUserId = (event.userId() != null && !event.userId().isBlank()) ? event.userId() : "unknown-user";

            DlqMessageEntity entity = DlqMessageEntity.builder()
                    .eventId(safeEventId)
                    .correlationId(safeCorrelationId)
                    .userId(safeUserId)
                    .fileName(event.fileName() != null ? event.fileName() : "arquivo-desconhecido")
                    .lineNumber(event.lineNumber())
                    .errorMessage(errorMessage)
                    .originalPayload(originalPayloadJson)
                    .originalTopic(TopicConstants.TRADE_EXTRACTED_TOPIC)
                    .status(DlqMessageStatus.PENDING)
                    .createdAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                    .updatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                    .build();

            DlqMessageEntity saved = dlqMessageRepository.save(entity);
            log.info("Mensagem DLQ persistida com sucesso. ID: {}, eventId: {}", saved.getId(), saved.getEventId());
        } catch (Exception ex) {
            log.error("Erro ao persistir mensagem DLQ: {}", ex.getMessage(), ex);
        }
    }
}
