package afsdigital.grahamselect.api.dividends.infrastructure.crawler;

import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvmOfficialCrawlerAdapterTest {

    @Test
    @DisplayName("Deve extrair proventos de documento HTML da CVM/B3 hermeticamente")
    void shouldExtractDividendsFromCvmHtml() {
        String html = """
                <html>
                <body>
                  <table class="tabelaProventos">
                    <thead>
                      <tr>
                        <th>Ticker</th><th>Tipo</th><th>Data COM</th><th>Data Pagamento</th><th>Valor Bruto</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>VALE3</td>
                        <td>DIVIDENDO</td>
                        <td>2026-03-12</td>
                        <td>2026-03-28</td>
                        <td>2,450000</td>
                      </tr>
                      <tr>
                        <td>VALE3</td>
                        <td>JCP</td>
                        <td>2026-06-10</td>
                        <td>2026-06-25</td>
                        <td>1,000000</td>
                      </tr>
                    </tbody>
                  </table>
                </body>
                </html>
                """;

        Document document = Jsoup.parse(html);
        CvmOfficialCrawlerAdapter adapter = new CvmOfficialCrawlerAdapter(url -> document, "TestAgent/1.0", 5000);

        List<DividendAnnouncement> announcements = adapter.crawlDividends(List.of("VALE3"));

        assertThat(announcements).hasSize(2);

        DividendAnnouncement first = announcements.get(0);
        assertThat(first.getTicker()).isEqualTo("VALE3");
        assertThat(first.getType()).isEqualTo(CorporateEventType.DIVIDENDO);
        assertThat(first.getComDate()).isEqualTo(LocalDate.of(2026, 3, 12));
        assertThat(first.getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 28));
        assertThat(first.getRate()).isEqualByComparingTo(new BigDecimal("2.450000"));

        DividendAnnouncement second = announcements.get(1);
        assertThat(second.getType()).isEqualTo(CorporateEventType.JCP);
        assertThat(second.getNetValue()).isEqualByComparingTo(new BigDecimal("0.850000"));
    }

    @Test
    @DisplayName("Deve extrair proventos com taxa formatada com ponto decimal sem corromper valor")
    void shouldExtractDividendsWithDotDecimalRate() {
        String html = """
                <html>
                <body>
                  <table>
                    <tbody>
                      <tr>
                        <td>BBAS3</td>
                        <td>DIVIDENDO</td>
                        <td>2026-03-12</td>
                        <td>2026-03-28</td>
                        <td>0.854000</td>
                      </tr>
                    </tbody>
                  </table>
                </body>
                </html>
                """;

        Document document = Jsoup.parse(html);
        CvmOfficialCrawlerAdapter adapter = new CvmOfficialCrawlerAdapter(url -> document, "TestAgent/1.0", 5000);

        List<DividendAnnouncement> announcements = adapter.crawlDividends(List.of("BBAS3"));

        assertThat(announcements).hasSize(1);
        assertThat(announcements.get(0).getRate()).isEqualByComparingTo(new BigDecimal("0.854000"));
    }

    @Test
    @DisplayName("Deve extrair fatos relevantes de documento HTML da CVM/B3 hermeticamente")
    void shouldExtractRelevantFactsFromCvmHtml() {
        String html = """
                <html>
                <body>
                  <div class="fatos-relevantes">
                    <article class="fato-item">
                      <span class="ticker">VALE3</span>
                      <span class="data">2026-03-10T14:30:00Z</span>
                      <span class="categoria">FATO_RELEVANTE</span>
                      <h3 class="titulo"><a href="https://cvm.gov.br/doc/val123">Aprovação de Programa de Recompra</a></h3>
                      <p class="resumo">Conselho de Administração aprovou novo programa de recompra de ações.</p>
                    </article>
                  </div>
                </body>
                </html>
                """;

        Document document = Jsoup.parse(html);
        CvmOfficialCrawlerAdapter adapter = new CvmOfficialCrawlerAdapter(url -> document, "TestAgent/1.0", 5000);

        List<RelevantFact> facts = adapter.crawlRelevantFacts(List.of("VALE3"));

        assertThat(facts).hasSize(1);
        RelevantFact fact = facts.get(0);
        assertThat(fact.getTicker()).isEqualTo("VALE3");
        assertThat(fact.getTitle()).isEqualTo("Aprovação de Programa de Recompra");
        assertThat(fact.getSourceUrl()).isEqualTo("https://cvm.gov.br/doc/val123");
        assertThat(fact.getCategory()).isEqualTo("FATO_RELEVANTE");
    }
}
