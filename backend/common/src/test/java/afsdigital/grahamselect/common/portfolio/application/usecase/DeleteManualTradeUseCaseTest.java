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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteManualTradeUseCaseTest {

    @Mock
    private TradePort tradePort;
    @Mock
    private ManualTradeAuditPort auditPort;
    @Mock
    private ValuationEventPort valuationEventPort;

    private DeleteManualTradeUseCase useCase;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        useCase = new DeleteManualTradeUseCase(tradePort, auditPort, valuationEventPort, objectMapper);
    }

    @Test
    void shouldDeleteTradeAuditAndPublishValuation() {
        UUID tradeId = UUID.randomUUID();
        Trade existing = Trade.builder()
                .id(tradeId)
                .userId("user-1")
                .ticker("HASH11")
                .side("COMPRA")
                .tradeDate(LocalDate.of(2026, 1, 10))
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("50.00"))
                .broker("Manual")
                .createdAt(LocalDateTime.now())
                .build();

        when(tradePort.findByIdAndUserId(tradeId, "user-1")).thenReturn(Optional.of(existing));

        boolean deleted = useCase.execute(tradeId, "user-1");

        assertTrue(deleted);
        verify(auditPort).save(any(ManualTradeAudit.class));
        verify(tradePort).deleteByIdAndUserId(tradeId, "user-1");
        verify(valuationEventPort).publishValuationRequest(any(ValuationRequestedEvent.class));
    }

    @Test
    void shouldReturnFalseWhenTradeNotFound() {
        UUID tradeId = UUID.randomUUID();
        when(tradePort.findByIdAndUserId(tradeId, "user-1")).thenReturn(Optional.empty());

        boolean deleted = useCase.execute(tradeId, "user-1");

        assertFalse(deleted);
        verify(tradePort, never()).deleteByIdAndUserId(any(), any());
        verify(auditPort, never()).save(any());
    }
}
