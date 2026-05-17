package afsdigital.grahamselect.common.upload.application.usecase;

import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.application.repository.TradeExtractionEventPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractionFailedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class ProcessB3FileUseCase {

    private final TradeExtractionEventPort tradeExtractionEventPort;
    private final B3ImportStatusPort importStatusPort;

    public ProcessB3FileUseCase(
            TradeExtractionEventPort tradeExtractionEventPort,
            B3ImportStatusPort importStatusPort
    ) {
        this.tradeExtractionEventPort = tradeExtractionEventPort;
        this.importStatusPort = importStatusPort;
    }

    public void startProcessing(FileUploadedEvent event) {
        importStatusPort.markProcessing(event.correlationId(), event.userId());
    }

    public void handleExtractedTrade(FileUploadedEvent event, B3TradeRow tradeRow) {
        TradeExtractedEvent tradeExtractedEvent = TradeExtractedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(event.userId())
                .correlationId(event.correlationId())
                .fileName(event.fileName())
                .lineNumber(tradeRow.lineNumber())
                .ticker(tradeRow.ticker())
                .tradeDate(tradeRow.tradeDate())
                .quantity(tradeRow.quantity())
                .price(tradeRow.price())
                .broker(tradeRow.broker())
                .build();

        tradeExtractionEventPort.publishExtractedTrade(tradeExtractedEvent);
    }

    public void handleExtractionFailure(FileUploadedEvent event, B3TradeRowFailure failure) {
        TradeExtractionFailedEvent failedEvent = TradeExtractionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(event.userId())
                .correlationId(event.correlationId())
                .fileName(event.fileName())
                .lineNumber(failure.lineNumber())
                .originalPayload(failure.originalPayload())
                .reason(failure.reason())
                .build();

        tradeExtractionEventPort.publishExtractionFailure(failedEvent);
    }

    public void updateProgress(FileUploadedEvent event, int addedSuccess, int addedFailure, String lastFailureReason) {
        importStatusPort.addProgress(event.correlationId(), event.userId(), addedSuccess, addedFailure, lastFailureReason);
    }

    public void completeProcessing(FileUploadedEvent event) {
        importStatusPort.markCompleted(event.correlationId(), event.userId());
    }

    public void failProcessing(FileUploadedEvent event, String failureReason) {
        importStatusPort.markFailed(event.correlationId(), event.userId(), failureReason);
    }
}
