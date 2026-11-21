package afsdigital.grahamselect.common.dividends.application.usecase;

import afsdigital.grahamselect.common.dividends.application.dto.CorporateEventSyncSummaryDTO;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAlertPublisherPort;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncCorporateEventsUseCaseTest {

    @Mock
    private CustodyTickerPort custodyTickerPort;

    @Mock
    private DividendCrawlerPort dividendCrawlerPort;

    @Mock
    private RelevantFactCrawlerPort relevantFactCrawlerPort;

    @Mock
    private DividendAnnouncementPort dividendAnnouncementPort;

    @Mock
    private RelevantFactPort relevantFactPort;

    @Mock
    private DividendAlertPublisherPort dividendAlertPublisherPort;

    @InjectMocks
    private SyncCorporateEventsUseCase useCase;

    @Test
    @DisplayName("Deve sincronizar proventos e fatos relevantes contabilizando novos e atualizados")
    void shouldSyncCorporateEventsSuccessfully() {
        when(custodyTickerPort.findDistinctActiveTickers()).thenReturn(List.of("PETR4", "VALE3"));

        LocalDate comDate = LocalDate.of(2026, 4, 15);
        BigDecimal rate = new BigDecimal("1.250000");

        DividendAnnouncement ann1 = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.DIVIDENDO)
                .rate(rate)
                .comDate(comDate)
                .source("BRAPI")
                .status("APPROVED")
                .build();

        DividendAnnouncement ann2 = DividendAnnouncement.builder()
                .ticker("VALE3")
                .type(CorporateEventType.JCP)
                .rate(new BigDecimal("0.800000"))
                .comDate(comDate)
                .source("CVM")
                .status("APPROVED")
                .build();

        when(dividendCrawlerPort.crawlDividends(anyList())).thenReturn(List.of(ann1, ann2));

        // PETR4 já existia (atualização), VALE3 é novo
        when(dividendAnnouncementPort.existsByTickerAndComDateAndTypeAndRate("PETR4", comDate, CorporateEventType.DIVIDENDO, rate))
                .thenReturn(true);
        when(dividendAnnouncementPort.existsByTickerAndComDateAndTypeAndRate("VALE3", comDate, CorporateEventType.JCP, new BigDecimal("0.800000")))
                .thenReturn(false);

        RelevantFact fact = RelevantFact.builder()
                .ticker("PETR4")
                .title("Aprovação de Proventos")
                .publicationDate(Instant.now())
                .build();

        when(relevantFactCrawlerPort.crawlRelevantFacts(anyList())).thenReturn(List.of(fact));
        when(relevantFactPort.existsByTickerAndPublicationDate(eq("PETR4"), any(Instant.class))).thenReturn(false);

        CorporateEventSyncSummaryDTO summary = useCase.execute();

        assertThat(summary).isNotNull();
        assertThat(summary.tickersProcessed()).isEqualTo(2);
        assertThat(summary.newDividends()).isEqualTo(1);
        assertThat(summary.updatedDividends()).isEqualTo(1);
        assertThat(summary.newFacts()).isEqualTo(1);
        assertThat(summary.status()).isEqualTo("SUCCESS");

        verify(dividendAnnouncementPort, times(2)).save(any(DividendAnnouncement.class));
        verify(relevantFactPort, times(1)).save(any(RelevantFact.class));
        verify(dividendAlertPublisherPort, times(1)).publishNewDividendAnnouncements(anyList());
    }
}
