package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.api.upload.infrastructure.parser.FastExcelB3TradeRowParser;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.application.usecase.ProcessB3FileUseCase;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class KafkaB3FileUploadedConsumer {

    private final ObjectMapper objectMapper;
    private final FastExcelB3TradeRowParser tradeRowParser;
    private final ProcessB3FileUseCase processB3FileUseCase;

    @Value("${app.upload.storage-path:./uploads}")
    private String baseStoragePath;

    @KafkaListener(topics = TopicConstants.B3_UPLOAD_TOPIC, groupId = "api-group")
    public void consume(String payload) throws IOException {
        FileUploadedEvent event = deserialize(payload);
        
        // 1. Path Traversal Protection
        Path filePath = Path.of(event.storagePath()).normalize().toAbsolutePath();
        Path baseDir = Path.of(baseStoragePath).normalize().toAbsolutePath();
        
        if (!filePath.startsWith(baseDir)) {
            processB3FileUseCase.failProcessing(event, "Tentativa de acesso a caminho inválido detectada.");
            return;
        }

        processB3FileUseCase.startProcessing(event);

        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();
        AtomicReference<String> lastFailureReason = new AtomicReference<>();

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            tradeRowParser.parse(
                    inputStream,
                    tradeRow -> {
                        processB3FileUseCase.handleExtractedTrade(event, tradeRow);
                        if (successCounter.incrementAndGet() % 100 == 0) {
                            processB3FileUseCase.updateProgress(event, successCounter.getAndSet(0), failureCounter.getAndSet(0), lastFailureReason.get());
                        }
                    },
                    failure -> {
                        processB3FileUseCase.handleExtractionFailure(event, failure);
                        lastFailureReason.set(failure.reason());
                        if (failureCounter.incrementAndGet() % 100 == 0) {
                            processB3FileUseCase.updateProgress(event, successCounter.getAndSet(0), failureCounter.getAndSet(0), lastFailureReason.get());
                        }
                    }
            );
            processB3FileUseCase.completeProcessing(event);
        } catch (Exception exception) {
            // Save remaining progress before failing
            if (successCounter.get() > 0 || failureCounter.get() > 0) {
                processB3FileUseCase.updateProgress(event, successCounter.get(), failureCounter.get(), lastFailureReason.get());
            }
            processB3FileUseCase.failProcessing(
                    event,
                    exception.getMessage() == null ? "Falha ao processar arquivo B3." : exception.getMessage()
            );
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
        } finally {
            // Ensure any residual progress is flushed if not already done (redundant but safe)
            if (successCounter.get() > 0 || failureCounter.get() > 0) {
                processB3FileUseCase.updateProgress(event, successCounter.getAndSet(0), failureCounter.getAndSet(0), lastFailureReason.get());
            }
        }
    }

    private FileUploadedEvent deserialize(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, FileUploadedEvent.class);
    }
}
