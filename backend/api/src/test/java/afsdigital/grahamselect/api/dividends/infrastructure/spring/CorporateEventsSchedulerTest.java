package afsdigital.grahamselect.api.dividends.infrastructure.spring;

import afsdigital.grahamselect.common.dividends.application.dto.CorporateEventSyncSummaryDTO;
import afsdigital.grahamselect.common.dividends.application.usecase.SyncCorporateEventsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorporateEventsSchedulerTest {

    @Mock
    private SyncCorporateEventsUseCase syncCorporateEventsUseCase;

    @InjectMocks
    private CorporateEventsScheduler scheduler;

    @Test
    @DisplayName("Deve invocar SyncCorporateEventsUseCase quando o agendamento for acionado")
    void shouldTriggerSyncWhenSchedulerFires() {
        CorporateEventSyncSummaryDTO summary = new CorporateEventSyncSummaryDTO(
                10, 5, 2, 3, Instant.now(), "SUCCESS"
        );
        when(syncCorporateEventsUseCase.execute()).thenReturn(summary);

        scheduler.syncCorporateEvents();

        verify(syncCorporateEventsUseCase, times(1)).execute();
    }

    @Test
    @DisplayName("Deve capturar e logar exceção sem quebrar execução do scheduler")
    void shouldHandleExceptionGracefully() {
        when(syncCorporateEventsUseCase.execute()).thenThrow(new RuntimeException("Connection error"));

        scheduler.syncCorporateEvents();

        verify(syncCorporateEventsUseCase, times(1)).execute();
    }
}
