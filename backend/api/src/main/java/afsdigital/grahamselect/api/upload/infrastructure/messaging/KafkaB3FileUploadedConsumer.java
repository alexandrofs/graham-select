package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.api.upload.infrastructure.parser.FastExcelB3TradeRowParser;
import afsdigital.grahamselect.api.upload.infrastructure.parser.SinacorPdfTradeParser;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.application.usecase.ProcessB3FileUseCase;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaB3FileUploadedConsumer {

    private final ObjectMapper objectMapper;
    private final FastExcelB3TradeRowParser tradeRowParser;
    private final SinacorPdfTradeParser sinacorPdfTradeParser;
    private final ProcessB3FileUseCase processB3FileUseCase;

    @Value("${app.upload.storage-path:./uploads}")
    private String baseStoragePath;

    @KafkaListener(topics = TopicConstants.B3_UPLOAD_TOPIC, groupId = "api-group")
    public void consume(String payload) throws IOException {
        FileUploadedEvent event = deserialize(payload);
        log.info("Iniciando consumo de arquivo de upload: correlationId={}, userId={}, profileId={}, fileName={}",
                event.correlationId(), event.userId(), event.profileId(), event.fileName());
        
        // 1. Path Traversal Protection
        Path filePath = Path.of(event.storagePath()).normalize().toAbsolutePath();
        Path baseDir = Path.of(baseStoragePath).normalize().toAbsolutePath();
        
        if (!filePath.startsWith(baseDir)) {
            log.error("Tentativa de acesso a caminho inválido detectada: storagePath={}, correlationId={}",
                    event.storagePath(), event.correlationId());
            processB3FileUseCase.failProcessing(event, "Tentativa de acesso a caminho inválido detectada.");
            return;
        }

        processB3FileUseCase.startProcessing(event);

        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();
        AtomicReference<String> lastFailureReason = new AtomicReference<>();

        boolean isPdf = event.fileName() != null && event.fileName().toLowerCase(Locale.ROOT).endsWith(".pdf");

        java.util.function.Consumer<afsdigital.grahamselect.common.upload.domain.model.B3TradeRow> onTradeExtracted = tradeRow -> {
            processB3FileUseCase.handleExtractedTrade(event, tradeRow);
            if (successCounter.incrementAndGet() % 100 == 0) {
                processB3FileUseCase.updateProgress(event, 100, 0, null);
                successCounter.addAndGet(-100);
            }
        };

        java.util.function.Consumer<afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure> onTradeFailure = failure -> {
            processB3FileUseCase.handleExtractionFailure(event, failure);
            lastFailureReason.set(failure.reason());
            if (failureCounter.incrementAndGet() % 100 == 0) {
                processB3FileUseCase.updateProgress(event, 0, 100, failure.reason());
                failureCounter.addAndGet(-100);
            }
        };

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            if (isPdf) {
                sinacorPdfTradeParser.parse(inputStream, onTradeExtracted, onTradeFailure);
            } else {
                tradeRowParser.parse(inputStream, onTradeExtracted, onTradeFailure);
            }
            processB3FileUseCase.completeProcessing(event);
            log.info("Processamento concluído com sucesso: correlationId={}, fileName={}",
                    event.correlationId(), event.fileName());
        } catch (Exception exception) {
            log.error("Erro ao processar arquivo no KafkaB3FileUploadedConsumer: correlationId={}, fileName={}, error={}",
                    event.correlationId(), event.fileName(), exception.getMessage(), exception);
            int remainingSuccess = successCounter.getAndSet(0);
            int remainingFailure = failureCounter.getAndSet(0);
            if (remainingSuccess > 0 || remainingFailure > 0) {
                processB3FileUseCase.updateProgress(event, remainingSuccess, remainingFailure, lastFailureReason.get());
            }
            processB3FileUseCase.failProcessing(
                    event,
                    exception.getMessage() == null ? "Falha ao processar arquivo B3." : exception.getMessage()
            );
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
        } finally {
            int remainingSuccess = successCounter.getAndSet(0);
            int remainingFailure = failureCounter.getAndSet(0);
            if (remainingSuccess > 0 || remainingFailure > 0) {
                processB3FileUseCase.updateProgress(event, remainingSuccess, remainingFailure, lastFailureReason.get());
            }
        }
    }

    private FileUploadedEvent deserialize(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, FileUploadedEvent.class);
    }
}
