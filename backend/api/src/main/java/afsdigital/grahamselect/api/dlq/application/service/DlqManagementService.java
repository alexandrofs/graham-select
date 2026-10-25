package afsdigital.grahamselect.api.dlq.application.service;

import afsdigital.grahamselect.api.admin.util.AdminAnonymizationUtils;
import afsdigital.grahamselect.api.dlq.application.dto.DlqBatchReprocessResultDto;
import afsdigital.grahamselect.api.dlq.application.dto.DlqMessageDto;
import afsdigital.grahamselect.api.dlq.application.dto.DlqReprocessResultDto;
import afsdigital.grahamselect.api.dlq.application.service.exceptions.DlqMessageNotFoundException;
import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.entities.DlqMessageEntity;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.repositories.DlqMessageRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqManagementService {

    private final DlqMessageRepository dlqMessageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DlqMessageDto> getPendingFailures(DlqMessageStatus status) {
        DlqMessageStatus targetStatus = status != null ? status : DlqMessageStatus.PENDING;
        log.info("Buscando falhas DLQ com status: {}", targetStatus);
        List<DlqMessageEntity> entities = dlqMessageRepository.findByStatusOrderByCreatedAtDesc(targetStatus);
        return entities.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DlqReprocessResultDto reprocess(Long id) {
        log.info("Iniciando reprocessamento da mensagem DLQ id: {}", id);
        DlqMessageEntity entity = dlqMessageRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Mensagem DLQ id {} não encontrada no repositório", id);
                    return new DlqMessageNotFoundException(id);
                });

        if (entity.getStatus() == DlqMessageStatus.RESOLVED) {
            log.info("Mensagem DLQ id: {} já foi resolvida anteriormente", id);
            return new DlqReprocessResultDto(id, DlqMessageStatus.RESOLVED, "Mensagem já resolvida anteriormente.");
        }

        try {
            if (entity.getOriginalPayload() == null || entity.getOriginalPayload().isBlank()) {
                log.error("Payload original inexistente ou vazio para a mensagem DLQ id: {}", id);
                throw new IllegalArgumentException("Payload original inexistente ou vazio para a mensagem DLQ id: " + id);
            }

            Map<String, String> payloadMap = objectMapper.readValue(
                    entity.getOriginalPayload(),
                    new TypeReference<Map<String, String>>() {}
            );

            String rawTicker = findValue(payloadMap, "ticker", "codigo", "ativo", "papel");
            if (rawTicker == null || rawTicker.isBlank()) {
                log.error("Ticker ausente no payload original da mensagem DLQ id: {}", id);
                throw new IllegalArgumentException("Ticker ausente no payload original");
            }
            String ticker = rawTicker.toUpperCase().trim();

            String rawDate = findValue(payloadMap, "data", "tradeDate", "dataOperacao", "date");
            LocalDate tradeDate = parseDate(rawDate);

            String rawQuantity = findValue(payloadMap, "quantidade", "quantity", "qtd");
            BigDecimal quantity = parseNumber(rawQuantity, "quantidade");

            String rawPrice = findValue(payloadMap, "preco", "price", "precoMedio", "valor");
            BigDecimal price = parseNumber(rawPrice, "preco");

            String rawSide = findValue(payloadMap, "side", "tipo", "operacao", "tipoOperacao");
            String side = "COMPRA";
            if (rawSide != null && !rawSide.isBlank()) {
                String upper = rawSide.toUpperCase().trim();
                side = ("V".equals(upper) || "VENDA".equals(upper)) ? "VENDA" : "COMPRA";
            }

            String rawBroker = findValue(payloadMap, "corretora", "broker", "instituicao");
            String broker = (rawBroker != null && !rawBroker.isBlank()) ? rawBroker.trim() : "B3";

            TradeExtractedEvent tradeEvent = TradeExtractedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .version("v1")
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                    .userId(entity.getUserId())
                    .correlationId(entity.getCorrelationId())
                    .fileName(entity.getFileName())
                    .lineNumber(entity.getLineNumber() != null ? entity.getLineNumber() : 1L)
                    .ticker(ticker)
                    .tradeDate(tradeDate)
                    .side(side)
                    .quantity(quantity)
                    .price(price)
                    .broker(broker)
                    .build();

            String partitionKey = tradeEvent.userId() + ":" + tradeEvent.ticker();
            kafkaTemplate.send(TopicConstants.TRADE_EXTRACTED_TOPIC, partitionKey, tradeEvent).get(5, TimeUnit.SECONDS);

            entity.setStatus(DlqMessageStatus.RESOLVED);
            entity.setResolvedAt(LocalDateTime.now(ZoneOffset.UTC));
            dlqMessageRepository.save(entity);

            log.info("Mensagem DLQ id: {} reprocessada com sucesso e publicada no tópico {}", id, TopicConstants.TRADE_EXTRACTED_TOPIC);
            return new DlqReprocessResultDto(id, DlqMessageStatus.RESOLVED, "Mensagem reprocessada com sucesso.");
        } catch (Exception ex) {
            log.error("Falha ao reprocessar mensagem DLQ id: {}: {}", id, ex.getMessage(), ex);
            entity.setStatus(DlqMessageStatus.FAILED);
            String originalError = entity.getErrorMessage() != null ? entity.getErrorMessage() : "";
            String errorMsg = "Falha de reprocessamento: " + ex.getMessage()
                    + (originalError.isBlank() ? "" : " (Erro original: " + originalError + ")");
            if (errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000);
            }
            entity.setErrorMessage(errorMsg);
            dlqMessageRepository.save(entity);
            return new DlqReprocessResultDto(id, DlqMessageStatus.FAILED, ex.getMessage());
        }
    }

    public DlqBatchReprocessResultDto reprocessBatch(List<Long> ids) {
        log.info("Iniciando reprocessamento em lote. IDs informados: {}", ids);
        List<Long> targetIds = ids != null ? ids.stream().filter(Objects::nonNull).distinct().toList() : null;
        if (targetIds == null || targetIds.isEmpty()) {
            List<DlqMessageEntity> pending = dlqMessageRepository.findByStatusOrderByCreatedAtDesc(DlqMessageStatus.PENDING);
            targetIds = pending.stream().map(DlqMessageEntity::getId).limit(100).toList();
            log.info("Nenhum ID específico fornecido. Encontrados {} registros PENDING para reprocessar (limitado a 100)", targetIds.size());
        }

        List<DlqReprocessResultDto> details = new ArrayList<>();
        for (Long id : targetIds) {
            try {
                DlqReprocessResultDto result = reprocess(id);
                details.add(result);
            } catch (Exception ex) {
                log.error("Erro inesperado ao reprocessar item id {} no lote: {}", id, ex.getMessage(), ex);
                details.add(new DlqReprocessResultDto(id, DlqMessageStatus.FAILED, ex.getMessage()));
            }
        }

        int totalProcessed = details.size();
        int successCount = (int) details.stream().filter(d -> d.status() == DlqMessageStatus.RESOLVED).count();
        int failureCount = totalProcessed - successCount;

        log.info("Reprocessamento em lote concluído. Total: {}, Sucessos: {}, Falhas: {}", totalProcessed, successCount, failureCount);
        return new DlqBatchReprocessResultDto(totalProcessed, successCount, failureCount, details);
    }

    private DlqMessageDto toDto(DlqMessageEntity entity) {
        OffsetDateTime timestamp = entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime resolvedAt = entity.getResolvedAt() != null ? entity.getResolvedAt().atOffset(ZoneOffset.UTC) : null;
        return new DlqMessageDto(
                entity.getId(),
                entity.getEventId(),
                entity.getCorrelationId(),
                AdminAnonymizationUtils.anonymizeUserId(entity.getUserId()),
                entity.getFileName(),
                entity.getLineNumber(),
                entity.getErrorMessage(),
                entity.getOriginalTopic(),
                entity.getStatus(),
                timestamp,
                resolvedAt
        );
    }

    private String findValue(Map<String, String> map, String... keys) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            log.error("Data da operação ausente no payload");
            throw new IllegalArgumentException("Data da operação ausente no payload");
        }
        String trimmed = rawDate.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e2) {
                log.error("Formato de data inválido para {}: {}", trimmed, e2.getMessage(), e2);
                throw new IllegalArgumentException("Formato de data inválido: " + trimmed + ". Esperado dd/MM/yyyy ou yyyy-MM-dd");
            }
        }
    }

    private BigDecimal parseNumber(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            log.error("Campo {} ausente no payload", fieldName);
            throw new IllegalArgumentException("Campo " + fieldName + " ausente no payload");
        }
        String cleaned = raw.replaceAll("[^0-9,.-]", "").trim();
        int lastDot = cleaned.lastIndexOf('.');
        int lastComma = cleaned.lastIndexOf(',');
        if (lastDot > -1 && lastComma > -1) {
            if (lastDot > lastComma) {
                // Formato en-US (ex: 1,500.50)
                cleaned = cleaned.replace(",", "");
            } else {
                // Formato pt-BR (ex: 1.500,50)
                cleaned = cleaned.replace(".", "").replace(',', '.');
            }
        } else if (lastComma > -1) {
            // Formato com vírgula decimal (ex: 68,50)
            cleaned = cleaned.replace(',', '.');
        }
        try {
            BigDecimal value = new BigDecimal(cleaned).setScale(4, RoundingMode.HALF_UP);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Campo {} deve ser maior que zero: {}", fieldName, raw);
                throw new IllegalArgumentException("Campo " + fieldName + " deve ser maior que zero: " + raw);
            }
            return value;
        } catch (NumberFormatException nfe) {
            log.error("Formato numérico inválido para {}: {}", fieldName, raw, nfe);
            throw new IllegalArgumentException("Formato numérico inválido para " + fieldName + ": " + raw);
        }
    }
}
