package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.events.ValuationRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateManualTradeUseCaseTest {

    @Mock
    private TradePort tradePort;
    @Mock
    private ManualTradeAuditPort auditPort;
    @Mock
    private ValuationEventPort valuationEventPort;

    private UpdateManualTradeUseCase useCase;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        useCase = new UpdateManualTradeUseCase(tradePort, auditPort, valuationEventPort, objectMapper);
    }

    @Test
    void shouldUpdateTradeAuditAndPublishValuation() {
        UUID tradeId = UUID.randomUUID();
        Trade existing = Trade.builder()
                .id(tradeId)
                .userId("user-1")
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.of(2026, 1, 10))
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .broker("Manual")
                .createdAt(LocalDateTime.now())
                .build();

        Trade updated = Trade.builder()
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.of(2026, 1, 12))
                .quantity(new BigDecimal("20"))
                .price(new BigDecimal("31.00"))
                .broker("XP")
                .assetClass("Ações BR")
                .build();

        when(tradePort.findByIdAndUserId(tradeId, "user-1")).thenReturn(Optional.of(existing));
        when(tradePort.existsOtherWithSameSignature(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(tradePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateManualTradeUseCase.UpdateResult result = useCase.execute(tradeId, "user-1", updated);

        assertEquals(UpdateManualTradeUseCase.UpdateStatus.UPDATED, result.status());
        assertEquals("Ações BR", result.trade().getAssetClass());
        verify(auditPort).save(any(ManualTradeAudit.class));
        verify(valuationEventPort).publishValuationRequest(any(ValuationRequestedEvent.class));
    }

    @Test
    void shouldReturnNotFoundWhenTradeDoesNotExist() {
        UUID tradeId = UUID.randomUUID();
        when(tradePort.findByIdAndUserId(tradeId, "user-1")).thenReturn(Optional.empty());

        UpdateManualTradeUseCase.UpdateResult result = useCase.execute(tradeId, "user-1", Trade.builder().build());

        assertEquals(UpdateManualTradeUseCase.UpdateStatus.NOT_FOUND, result.status());
        assertNull(result.trade());
        verify(tradePort, never()).save(any());
    }

    @Test
    void shouldReturnConflictWhenDuplicateExists() {
        UUID tradeId = UUID.randomUUID();
        Trade existing = Trade.builder().id(tradeId).userId("user-1").ticker("PETR4").build();
        when(tradePort.findByIdAndUserId(tradeId, "user-1")).thenReturn(Optional.of(existing));
        when(tradePort.existsOtherWithSameSignature(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        UpdateManualTradeUseCase.UpdateResult result = useCase.execute(tradeId, "user-1", Trade.builder().ticker("PETR4").build());

        assertEquals(UpdateManualTradeUseCase.UpdateStatus.CONFLICT, result.status());
        verify(tradePort, never()).save(any());
    }
}
