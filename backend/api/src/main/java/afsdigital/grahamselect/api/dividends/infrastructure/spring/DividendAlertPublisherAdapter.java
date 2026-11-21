package afsdigital.grahamselect.api.dividends.infrastructure.spring;

import afsdigital.grahamselect.common.dividends.application.repository.DividendAlertPublisherPort;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.notifications.application.usecase.NotifyDividendAlertsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class DividendAlertPublisherAdapter implements DividendAlertPublisherPort {

    private final NotifyDividendAlertsUseCase notifyDividendAlertsUseCase;
    private final Executor executor;

    public DividendAlertPublisherAdapter(
            NotifyDividendAlertsUseCase notifyDividendAlertsUseCase,
            @Qualifier("dividendAlertExecutor") Executor executor
    ) {
        this.notifyDividendAlertsUseCase = notifyDividendAlertsUseCase;
        this.executor = executor;
    }

    @Override
    public void publishNewDividendAnnouncements(List<DividendAnnouncement> announcements) {
        if (announcements == null || announcements.isEmpty() || notifyDividendAlertsUseCase == null) {
            return;
        }

        List<DividendAnnouncement> snapshot = List.copyOf(announcements);
        log.info("Despachando notificação de {} novo(s) provento(s) via Executor Spring...", snapshot.size());
        executor.execute(() -> {
            try {
                notifyDividendAlertsUseCase.notifyForNewAnnouncements(snapshot);
            } catch (Exception e) {
                log.error("Erro ao executar envio assíncrono de notificações de novos proventos: {}", e.getMessage(), e);
            }
        });
    }
}
