package afsdigital.grahamselect.api.dividends.infrastructure.crawler;

import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactCrawlerPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class CvmOfficialCrawlerAdapter implements DividendCrawlerPort, RelevantFactCrawlerPort {

    @FunctionalInterface
    public interface JsoupDocumentFetcher {
        Document fetch(String url) throws Exception;
    }

    private final JsoupDocumentFetcher documentFetcher;
    private final String userAgent;
    private final int timeoutMs;

    public CvmOfficialCrawlerAdapter(String userAgent, int timeoutMs) {
        this(
                url -> Jsoup.connect(url).userAgent(userAgent).timeout(timeoutMs).get(),
                userAgent,
                timeoutMs
        );
    }

    public CvmOfficialCrawlerAdapter(JsoupDocumentFetcher documentFetcher, String userAgent, int timeoutMs) {
        this.documentFetcher = documentFetcher;
        this.userAgent = userAgent;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public List<DividendAnnouncement> crawlDividends(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Iniciando crawling CVM de proventos para tickers: {}", tickers);
        List<DividendAnnouncement> announcements = new ArrayList<>();

        for (String ticker : tickers) {
            if (ticker == null || ticker.isBlank()) continue;
            String cleanTicker = ticker.trim().toUpperCase();

            try {
                // Monta URL de consulta pública para o ticker
                String url = "https://sistemas.cvm.gov.br/proventos/" + cleanTicker;
                Document doc = documentFetcher.fetch(url);
                if (doc != null) {
                    announcements.addAll(parseDividendsFromDoc(doc, cleanTicker));
                }
            } catch (Exception e) {
                log.warn("Falha no crawling de proventos CVM para ticker: {}. Erro: {}", cleanTicker, e.getMessage());
            }
        }

        return announcements;
    }

    @Override
    public List<RelevantFact> crawlRelevantFacts(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Iniciando crawling CVM de fatos relevantes para tickers: {}", tickers);
        List<RelevantFact> facts = new ArrayList<>();

        for (String ticker : tickers) {
            if (ticker == null || ticker.isBlank()) continue;
            String cleanTicker = ticker.trim().toUpperCase();

            try {
                String url = "https://sistemas.cvm.gov.br/fatos-relevantes/" + cleanTicker;
                Document doc = documentFetcher.fetch(url);
                if (doc != null) {
                    facts.addAll(parseRelevantFactsFromDoc(doc, cleanTicker));
                }
            } catch (Exception e) {
                log.warn("Falha no crawling de fatos relevantes CVM para ticker: {}. Erro: {}", cleanTicker, e.getMessage());
            }
        }

        return facts;
    }

    private List<DividendAnnouncement> parseDividendsFromDoc(Document doc, String fallbackTicker) {
        List<DividendAnnouncement> list = new ArrayList<>();
        Elements rows = doc.select("table.tabelaProventos tbody tr, table tbody tr");

        for (Element row : rows) {
            try {
                Elements cols = row.select("td");
                if (cols.size() < 5) continue;

                String ticker = cols.get(0).text().trim().toUpperCase();
                if (ticker.isBlank()) ticker = fallbackTicker;

                String typeStr = cols.get(1).text().trim();
                CorporateEventType type = CorporateEventType.fromString(typeStr);

                LocalDate comDate = parseLocalDate(cols.get(2).text().trim());
                if (comDate == null) continue;

                LocalDate paymentDate = parseLocalDate(cols.get(3).text().trim());

                String rateStr = cols.get(4).text().replace("R$", "").replace(".", "").replace(",", ".").trim();
                BigDecimal rate = new BigDecimal(rateStr);

                String status = (paymentDate != null && paymentDate.isBefore(LocalDate.now(ZoneOffset.UTC)))
                        ? "PAID"
                        : "APPROVED";

                DividendAnnouncement announcement = DividendAnnouncement.builder()
                        .ticker(ticker)
                        .type(type)
                        .rate(rate)
                        .comDate(comDate)
                        .paymentDate(paymentDate)
                        .source("CVM")
                        .status(status)
                        .build();

                list.add(announcement);
            } catch (Exception e) {
                log.warn("Erro ao parsear linha de provento CVM: {}. Erro: {}", row.text(), e.getMessage());
            }
        }
        return list;
    }

    private List<RelevantFact> parseRelevantFactsFromDoc(Document doc, String fallbackTicker) {
        List<RelevantFact> list = new ArrayList<>();
        Elements items = doc.select(".fato-item, article.fato-item, .card-fato");

        for (Element item : items) {
            try {
                Element tickerEl = item.selectFirst(".ticker");
                String ticker = (tickerEl != null && !tickerEl.text().isBlank())
                        ? tickerEl.text().trim().toUpperCase()
                        : fallbackTicker;

                Element titleEl = item.selectFirst("h3 a, .titulo a, h3, .titulo");
                String title = titleEl != null ? titleEl.text().trim() : item.select("h3, h4, .titulo").text().trim();
                if (title.isBlank()) continue;

                Element linkEl = item.selectFirst("a[href]");
                String sourceUrl = null;
                if (linkEl != null) {
                    String absHref = linkEl.attr("abs:href");
                    sourceUrl = (absHref != null && !absHref.isBlank()) ? absHref : linkEl.attr("href");
                }

                Element summaryEl = item.selectFirst(".resumo, p");
                String summary = summaryEl != null ? summaryEl.text().trim() : null;

                Element catEl = item.selectFirst(".categoria, span.badge");
                String category = catEl != null && !catEl.text().isBlank()
                        ? catEl.text().trim().toUpperCase()
                        : "FATO_RELEVANTE";

                Element dateEl = item.selectFirst(".data, time");
                Instant pubDate = Instant.now();
                if (dateEl != null && !dateEl.text().isBlank()) {
                    pubDate = parseInstant(dateEl.text().trim());
                }

                RelevantFact fact = RelevantFact.builder()
                        .ticker(ticker)
                        .title(title)
                        .summary(summary)
                        .sourceUrl(sourceUrl)
                        .category(category)
                        .publicationDate(pubDate)
                        .build();

                list.add(fact);
            } catch (Exception e) {
                log.warn("Erro ao parsear fato relevante CVM: {}. Erro: {}", item.text(), e.getMessage());
            }
        }
        return list;
    }

    private LocalDate parseLocalDate(String str) {
        if (str == null || str.isBlank() || str.equals("-")) return null;
        try {
            if (str.contains("-")) {
                return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            log.warn("Data inválida no crawler CVM: {}", str);
            return null;
        }
    }

    private Instant parseInstant(String str) {
        if (str == null || str.isBlank()) return Instant.now();
        try {
            if (str.contains("T")) {
                return Instant.parse(str);
            }
            LocalDate ld = parseLocalDate(str);
            if (ld != null) {
                return ld.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        } catch (Exception e) {
            log.warn("Instant inválido no crawler CVM: {}", str);
        }
        return Instant.now();
    }
}
