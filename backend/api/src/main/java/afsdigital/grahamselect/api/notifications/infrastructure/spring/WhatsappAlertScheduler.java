package afsdigital.grahamselect.api.notifications.infrastructure.spring;

import afsdigital.grahamselect.common.notifications.application.usecase.EvaluateRebalanceAndGoalAlertsUseCase;
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
public class WhatsappAlertScheduler {

    private final EvaluateRebalanceAndGoalAlertsUseCase evaluateRebalanceAndGoalAlertsUseCase;

    @Scheduled(cron = "${whatsapp.alerts.cron:0 0 9 * * MON-FRI}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "WhatsappAlertScheduler_evaluateAlerts", lockAtLeastFor = "1m", lockAtMostFor = "15m")
    public void evaluateAlerts() {
        log.info("Disparando agendamento WhatsappAlertScheduler às {}", OffsetDateTime.now(ZoneOffset.UTC));
        try {
            evaluateRebalanceAndGoalAlertsUseCase.execute();
            log.info("Agendamento WhatsappAlertScheduler finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro durante a execução programada de WhatsappAlertScheduler: {}", e.getMessage(), e);
        }
    }
}
