package afsdigital.grahamselect.api.notifications.infrastructure.spring;

import afsdigital.grahamselect.common.notifications.application.usecase.EvaluateGrahamBuyZoneAlertsUseCase;
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
public class WebPushAlertScheduler {

    private final EvaluateGrahamBuyZoneAlertsUseCase evaluateGrahamBuyZoneAlertsUseCase;

    @Scheduled(cron = "${webpush.alerts.cron:0 15 20 * * MON-FRI}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "WebPushAlertScheduler_evaluateBuyZone", lockAtLeastFor = "1m", lockAtMostFor = "15m")
    public void evaluateBuyZoneAlerts() {
        log.info("Disparando agendamento WebPushAlertScheduler às {}", OffsetDateTime.now(ZoneOffset.UTC));
        try {
            evaluateGrahamBuyZoneAlertsUseCase.execute();
            log.info("Agendamento WebPushAlertScheduler finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro durante a execução programada de WebPushAlertScheduler: {}", e.getMessage(), e);
        }
    }
}
