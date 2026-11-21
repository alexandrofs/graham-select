package afsdigital.grahamselect.api.dividends.infrastructure.spring;

import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.notifications.application.usecase.NotifyDividendAlertsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DividendAlertPublisherAdapterTest {

    @Mock
    private NotifyDividendAlertsUseCase notifyDividendAlertsUseCase;

    @Test
    @DisplayName("Deve delegar publicacao de novos proventos ao executor gerenciado")
    void shouldDelegatePublishingToExecutor() {
        Executor directExecutor = new SyncTaskExecutor();
        DividendAlertPublisherAdapter adapter = new DividendAlertPublisherAdapter(notifyDividendAlertsUseCase, directExecutor);

        DividendAnnouncement ann = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.DIVIDENDO)
                .rate(new BigDecimal("1.50"))
                .comDate(LocalDate.now())
                .source("BRAPI")
                .build();

        adapter.publishNewDividendAnnouncements(List.of(ann));

        verify(notifyDividendAlertsUseCase, times(1)).notifyForNewAnnouncements(anyList());
    }

    @Test
    @DisplayName("Nao deve disparar notificacao quando lista for nula ou vazia")
    void shouldNotPublishWhenListIsNullOrEmpty() {
        Executor directExecutor = new SyncTaskExecutor();
        DividendAlertPublisherAdapter adapter = new DividendAlertPublisherAdapter(notifyDividendAlertsUseCase, directExecutor);

        adapter.publishNewDividendAnnouncements(null);
        adapter.publishNewDividendAnnouncements(List.of());

        verifyNoInteractions(notifyDividendAlertsUseCase);
    }
}
