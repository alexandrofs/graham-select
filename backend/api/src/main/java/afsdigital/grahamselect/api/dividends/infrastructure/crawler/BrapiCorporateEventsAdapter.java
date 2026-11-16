package afsdigital.grahamselect.api.dividends.infrastructure.crawler;

import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BrapiCorporateEventsAdapter implements DividendCrawlerPort {

    private final RestClient brapiRestClient;
    private final String token;

    public BrapiCorporateEventsAdapter(RestClient brapiRestClient, String token) {
        this.brapiRestClient = brapiRestClient;
        this.token = token;
    }

    @Override
    public List<DividendAnnouncement> crawlDividends(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> cleanTickers = tickers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (cleanTickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Iniciando coleta de eventos corporativos na Brapi para tickers: {}", cleanTickers);
        List<DividendAnnouncement> announcements = new ArrayList<>();

        int batchSize = 10;
        for (int i = 0; i < cleanTickers.size(); i += batchSize) {
            List<String> batch = cleanTickers.subList(i, Math.min(i + batchSize, cleanTickers.size()));
            String tickersParam = String.join(",", batch);

            try {
                Optional<BrapiDividendsResponse> responseOpt = fetchFromApi(tickersParam);
                responseOpt.ifPresent(response -> processDividends(response, announcements));
            } catch (Exception e) {
                log.error("Erro na consulta de dividendos na Brapi para lote: {}. Erro: {}", batch, e.getMessage(), e);
                // Retentativa individual para isolar erros
                if (batch.size() > 1) {
                    for (String ticker : batch) {
                        try {
                            fetchFromApi(ticker).ifPresent(res -> processDividends(res, announcements));
                        } catch (Exception ex) {
                            log.error("Erro na consulta de dividendos na Brapi para ticker individual: {}. Erro: {}", ticker, ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

        return announcements;
    }

    private Optional<BrapiDividendsResponse> fetchFromApi(String tickers) {
        try {
            BrapiDividendsResponse response = brapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote/" + tickers)
                            .queryParam("dividends", "true")
                            .queryParam("token", token)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Brapi API retornou status de erro: {} para tickers: {}", resp.getStatusCode(), tickers);
                    })
                    .body(BrapiDividendsResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Falha ao comunicar com Brapi API para tickers: {}", tickers, e);
            throw e;
        }
    }

    private void processDividends(BrapiDividendsResponse response, List<DividendAnnouncement> announcements) {
        if (response == null || response.results() == null) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (BrapiQuoteDividendResult result : response.results()) {
            if (result == null || result.dividendsData() == null || result.dividendsData().cashDividends() == null) {
                continue;
            }

            String fallbackTicker = result.symbol() != null ? cleanSymbol(result.symbol()) : "";

            for (BrapiCashDividend item : result.dividendsData().cashDividends()) {
                if (item == null || item.rate() == null || item.rate().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String ticker = item.assetIssued() != null && !item.assetIssued().isBlank()
                        ? cleanSymbol(item.assetIssued())
                        : fallbackTicker;

                if (ticker.isBlank()) {
                    continue;
                }

                LocalDate comDate = parseDate(item.lastDatePrior());
                if (comDate == null) {
                    continue;
                }

                LocalDate paymentDate = parseDate(item.paymentDate());
                LocalDate approvedDate = parseDate(item.approvedOn());
                CorporateEventType type = CorporateEventType.fromString(item.label());
                String status = (paymentDate != null && !paymentDate.isAfter(today)) ? "PAID" : "APPROVED";

                DividendAnnouncement announcement = DividendAnnouncement.builder()
                        .ticker(ticker)
                        .type(type)
                        .rate(item.rate())
                        .approvedDate(approvedDate)
                        .comDate(comDate)
                        .paymentDate(paymentDate)
                        .source("BRAPI")
                        .status(status)
                        .build();

                announcements.add(announcement);
            }
        }
    }

    private String cleanSymbol(String symbol) {
        if (symbol == null) return "";
        String clean = symbol.trim().toUpperCase();
        if (clean.endsWith(".SA")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean;
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            if (rawDate.contains("T")) {
                return Instant.parse(rawDate).atZone(ZoneOffset.UTC).toLocalDate();
            }
            return LocalDate.parse(rawDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            log.error("Não foi possível parsear data: {}", rawDate, e);
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiDividendsResponse(
            @JsonProperty("results") List<BrapiQuoteDividendResult> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiQuoteDividendResult(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("dividendsData") BrapiDividendsData dividendsData
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiDividendsData(
            @JsonProperty("cashDividends") List<BrapiCashDividend> cashDividends
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiCashDividend(
            @JsonProperty("assetIssued") String assetIssued,
            @JsonProperty("paymentDate") String paymentDate,
            @JsonProperty("rate") BigDecimal rate,
            @JsonProperty("approvedOn") String approvedOn,
            @JsonProperty("label") String label,
            @JsonProperty("lastDatePrior") String lastDatePrior
    ) {}
}
