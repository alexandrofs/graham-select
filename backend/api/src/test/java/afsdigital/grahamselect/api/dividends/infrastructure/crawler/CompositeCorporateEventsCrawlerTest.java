package afsdigital.grahamselect.api.dividends.infrastructure.crawler;

import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactCrawlerPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeCorporateEventsCrawlerTest {

    @Mock
    private DividendCrawlerPort brapiDividendCrawler;

    @Mock
    private DividendCrawlerPort cvmDividendCrawler;

    @Mock
    private RelevantFactCrawlerPort cvmRelevantFactCrawler;

    @Test
    @DisplayName("Deve agregar proventos de múltiplas fontes deduplicando pela chave (ticker, comDate, type, rate) e priorizando paymentDate não nulo")
    void shouldAggregateAndDeduplicateDividends() {
        CompositeCorporateEventsCrawler composite = new CompositeCorporateEventsCrawler(
                brapiDividendCrawler,
                cvmDividendCrawler,
                cvmRelevantFactCrawler
        );

        LocalDate comDate = LocalDate.of(2026, 4, 15);
        BigDecimal rate = new BigDecimal("1.250000");

        // CVM retorna sem payment_date definido
        DividendAnnouncement cvmAnn = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.DIVIDENDO)
                .rate(rate)
                .comDate(comDate)
                .paymentDate(null)
                .source("CVM")
                .status("APPROVED")
                .build();

        // Brapi retorna com payment_date definido
        DividendAnnouncement brapiAnn = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.DIVIDENDO)
                .rate(rate)
                .comDate(comDate)
                .paymentDate(LocalDate.of(2026, 5, 20))
                .source("BRAPI")
                .status("APPROVED")
                .build();

        when(brapiDividendCrawler.crawlDividends(anyList())).thenReturn(List.of(brapiAnn));
        when(cvmDividendCrawler.crawlDividends(anyList())).thenReturn(List.of(cvmAnn));

        List<DividendAnnouncement> aggregated = composite.crawlDividends(List.of("PETR4"));

        assertThat(aggregated).hasSize(1);
        assertThat(aggregated.get(0).getPaymentDate()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("Deve tolerar falha em uma fonte externa sem abortar execução do crawler")
    void shouldTolerateFailureInOneSourceGracefully() {
        CompositeCorporateEventsCrawler composite = new CompositeCorporateEventsCrawler(
                brapiDividendCrawler,
                cvmDividendCrawler,
                cvmRelevantFactCrawler
        );

        when(brapiDividendCrawler.crawlDividends(anyList())).thenThrow(new RuntimeException("API Timeout"));

        DividendAnnouncement cvmAnn = DividendAnnouncement.builder()
                .ticker("VALE3")
                .type(CorporateEventType.JCP)
                .rate(new BigDecimal("0.500000"))
                .comDate(LocalDate.of(2026, 6, 1))
                .source("CVM")
                .status("APPROVED")
                .build();

        when(cvmDividendCrawler.crawlDividends(anyList())).thenReturn(List.of(cvmAnn));

        List<DividendAnnouncement> aggregated = composite.crawlDividends(List.of("VALE3"));

        assertThat(aggregated).hasSize(1);
        assertThat(aggregated.get(0).getTicker()).isEqualTo("VALE3");
    }
}
