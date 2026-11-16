package afsdigital.grahamselect.api.dividends.infrastructure.crawler;

import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiCorporateEventsAdapterTest {

    @Test
    @DisplayName("Deve extrair dividendos da resposta JSON da Brapi hermeticamente")
    void shouldExtractDividendsFromBrapiJson() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brapi.dev/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        BrapiCorporateEventsAdapter adapter = new BrapiCorporateEventsAdapter(restClient, "test-token");

        String jsonResponse = """
                {
                  "results": [
                    {
                      "symbol": "PETR4",
                      "dividendsData": {
                        "cashDividends": [
                          {
                            "assetIssued": "PETR4",
                            "paymentDate": "2026-05-20T00:00:00.000Z",
                            "rate": 1.25,
                            "approvedOn": "2026-04-10T00:00:00.000Z",
                            "label": "DIVIDENDO",
                            "lastDatePrior": "2026-04-25T00:00:00.000Z"
                          },
                          {
                            "assetIssued": "PETR4",
                            "paymentDate": "2026-06-15T00:00:00.000Z",
                            "rate": 0.80,
                            "approvedOn": "2026-04-10T00:00:00.000Z",
                            "label": "JCP",
                            "lastDatePrior": "2026-05-02T00:00:00.000Z"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestTo("https://brapi.dev/api/quote/PETR4?dividends=true&token=test-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<DividendAnnouncement> announcements = adapter.crawlDividends(List.of("PETR4"));

        assertThat(announcements).hasSize(2);

        DividendAnnouncement div = announcements.get(0);
        assertThat(div.getTicker()).isEqualTo("PETR4");
        assertThat(div.getType()).isEqualTo(CorporateEventType.DIVIDENDO);
        assertThat(div.getRate()).isEqualByComparingTo(new BigDecimal("1.250000"));
        assertThat(div.getNetValue()).isEqualByComparingTo(new BigDecimal("1.250000"));
        assertThat(div.getComDate()).isEqualTo(LocalDate.of(2026, 4, 25));
        assertThat(div.getPaymentDate()).isEqualTo(LocalDate.of(2026, 5, 20));

        DividendAnnouncement jcp = announcements.get(1);
        assertThat(jcp.getTicker()).isEqualTo("PETR4");
        assertThat(jcp.getType()).isEqualTo(CorporateEventType.JCP);
        assertThat(jcp.getRate()).isEqualByComparingTo(new BigDecimal("0.800000"));
        assertThat(jcp.getNetValue()).isEqualByComparingTo(new BigDecimal("0.680000")); // 0.80 * 0.85
        assertThat(jcp.getComDate()).isEqualTo(LocalDate.of(2026, 5, 2));
    }
}
