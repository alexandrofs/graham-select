package afsdigital.grahamselect.common.upload.application.usecase;

import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.application.repository.TradeExtractionEventPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessB3FileUseCaseTest {

    @Mock
    private TradeExtractionEventPort tradeExtractionEventPort;

    @Mock
    private B3ImportStatusPort importStatusPort;

    @InjectMocks
    private ProcessB3FileUseCase useCase;

    @Test
    void shouldPublishExtractedTradeAndIncrementStatus() {
        FileUploadedEvent event = FileUploadedEvent.builder()
                .eventId("event-1")
                .userId("user-123")
                .fileName("b3.xlsx")
                .storagePath("/tmp/b3.xlsx")
                .correlationId("corr-123")
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        B3TradeRow tradeRow = new B3TradeRow(
                2L,
                "PETR4",
                LocalDate.of(2026, 5, 17),
                "COMPRA",
                BigDecimal.valueOf(100),
                new BigDecimal("29.90"),
                "XP"
        );

        useCase.handleExtractedTrade(event, tradeRow);

        verify(tradeExtractionEventPort).publishExtractedTrade(argThat(publishedEvent ->
                publishedEvent.correlationId().equals("corr-123")
                        && publishedEvent.userId().equals("user-123")
                        && publishedEvent.ticker().equals("PETR4")
                        && publishedEvent.lineNumber() == 2L
                        && publishedEvent.eventId() != null
        ));
    }

    @Test
    void shouldPublishDlqEventAndIncrementFailureStatus() {
        FileUploadedEvent event = FileUploadedEvent.builder()
                .eventId("event-1")
                .userId("user-123")
                .fileName("b3.xlsx")
                .storagePath("/tmp/b3.xlsx")
                .correlationId("corr-123")
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        B3TradeRowFailure failure = new B3TradeRowFailure(
                5L,
                Map.of("ticker", "PETR4", "data", "inválida"),
                "Data inválida"
        );

        useCase.handleExtractionFailure(event, failure);

        verify(tradeExtractionEventPort).publishExtractionFailure(argThat(publishedEvent ->
                publishedEvent.correlationId().equals("corr-123")
                        && publishedEvent.userId().equals("user-123")
                        && publishedEvent.lineNumber() == 5L
                        && publishedEvent.originalPayload().containsKey("ticker")
        ));
    }

    @Test
    void shouldUpdateProgress() {
        FileUploadedEvent event = FileUploadedEvent.builder()
                .eventId("event-1")
                .userId("user-123")
                .fileName("b3.xlsx")
                .storagePath("/tmp/b3.xlsx")
                .correlationId("corr-123")
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        useCase.updateProgress(event, 50, 10, "Erro na data");

        verify(importStatusPort).addProgress("corr-123", "user-123", 50, 10, "Erro na data");
    }
}
