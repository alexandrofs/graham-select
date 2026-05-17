package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.events.ValuationRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateManualTradeUseCaseTest {

    @Mock
    private TradePort tradePort;
    @Mock
    private ManualTradeAuditPort auditPort;
    @Mock
    private ValuationEventPort valuationEventPort;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private CreateManualTradeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateManualTradeUseCase(tradePort, auditPort, valuationEventPort, objectMapper);
    }

    @Test
    void shouldCreateManualTradeAndAuditAndEvent() {
        Trade trade = Trade.builder()
                .id(UUID.randomUUID())
                .userId("user1")
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.now())
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("35.50"))
                .broker("Inter")
                .createdAt(LocalDateTime.now())
                .build();

        when(tradePort.exists(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(tradePort.save(any())).thenReturn(trade);

        Trade result = useCase.execute(trade);

        assertNotNull(result);
        assertEquals(trade.getId(), result.getId());

        verify(tradePort).save(trade);
        verify(auditPort).save(any(ManualTradeAudit.class));
        verify(valuationEventPort).publishValuationRequest(any(ValuationRequestedEvent.class));
    }

    @Test
    void shouldReturnNullWhenTradeAlreadyExists() {
        Trade trade = Trade.builder()
                .userId("user1")
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.now())
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("35.50"))
                .broker("Inter")
                .build();

        when(tradePort.exists(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);

        Trade result = useCase.execute(trade);

        assertNull(result);
        verify(tradePort, never()).save(any());
        verify(auditPort, never()).save(any());
        verify(valuationEventPort, never()).publishValuationRequest(any());
    }
}
