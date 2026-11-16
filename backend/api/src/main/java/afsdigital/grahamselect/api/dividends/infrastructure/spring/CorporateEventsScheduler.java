package afsdigital.grahamselect.api.dividends.infrastructure.spring;

import afsdigital.grahamselect.common.dividends.application.dto.CorporateEventSyncSummaryDTO;
import afsdigital.grahamselect.common.dividends.application.usecase.SyncCorporateEventsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorporateEventsScheduler {

    private final SyncCorporateEventsUseCase syncCorporateEventsUseCase;

    @Scheduled(cron = "${corporate-events.sync.cron:0 0 21 * * MON-FRI}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "CorporateEventsScheduler_syncCorporateEvents", lockAtLeastFor = "1m", lockAtMostFor = "15m")
    public void syncCorporateEvents() {
        log.info("Disparando agendamento CorporateEventsScheduler às {}", OffsetDateTime.now(ZoneOffset.UTC));
        try {
            CorporateEventSyncSummaryDTO summary = syncCorporateEventsUseCase.execute();
            log.info("Agendamento CorporateEventsScheduler finalizado com sucesso. Métricas consolidadas: tickersProcessados={}, novosProventos={}, proventosAtualizados={}, novosFatos={}",
                    summary.tickersProcessed(),
                    summary.newDividends(),
                    summary.updatedDividends(),
                    summary.newFacts());
        } catch (Exception e) {
            log.error("Erro durante a execução programada de CorporateEventsScheduler: {}", e.getMessage(), e);
        }
    }
}
