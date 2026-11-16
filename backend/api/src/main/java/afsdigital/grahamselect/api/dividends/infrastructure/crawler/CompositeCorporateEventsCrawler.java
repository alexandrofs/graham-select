package afsdigital.grahamselect.api.dividends.infrastructure.crawler;

import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactCrawlerPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CompositeCorporateEventsCrawler implements DividendCrawlerPort, RelevantFactCrawlerPort {

    private final DividendCrawlerPort brapiDividendCrawler;
    private final DividendCrawlerPort cvmDividendCrawler;
    private final RelevantFactCrawlerPort cvmRelevantFactCrawler;

    public CompositeCorporateEventsCrawler(
            DividendCrawlerPort brapiDividendCrawler,
            DividendCrawlerPort cvmDividendCrawler,
            RelevantFactCrawlerPort cvmRelevantFactCrawler
    ) {
        this.brapiDividendCrawler = brapiDividendCrawler;
        this.cvmDividendCrawler = cvmDividendCrawler;
        this.cvmRelevantFactCrawler = cvmRelevantFactCrawler;
    }

    private record DividendKey(String ticker, LocalDate comDate, CorporateEventType type, BigDecimal rate) {}

    @Override
    public List<DividendAnnouncement> crawlDividends(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Iniciando coleta composta de proventos para {} tickers", tickers.size());
        Map<DividendKey, DividendAnnouncement> consolidated = new LinkedHashMap<>();

        // 1. Coleta na Brapi
        if (brapiDividendCrawler != null) {
            try {
                List<DividendAnnouncement> brapiList = brapiDividendCrawler.crawlDividends(tickers);
                mergeDividends(consolidated, brapiList);
            } catch (Exception e) {
                log.warn("Erro ao coletar proventos na Brapi: {}", e.getMessage(), e);
            }
        }

        // 2. Coleta na CVM (canais oficiais)
        if (cvmDividendCrawler != null) {
            try {
                List<DividendAnnouncement> cvmList = cvmDividendCrawler.crawlDividends(tickers);
                mergeDividends(consolidated, cvmList);
            } catch (Exception e) {
                log.warn("Erro ao coletar proventos nos canais oficiais CVM: {}", e.getMessage(), e);
            }
        }

        return new ArrayList<>(consolidated.values());
    }

    private void mergeDividends(Map<DividendKey, DividendAnnouncement> target, List<DividendAnnouncement> source) {
        if (source == null) return;
        for (DividendAnnouncement ann : source) {
            if (ann == null) continue;
            DividendKey key = new DividendKey(
                    ann.getTicker().toUpperCase(),
                    ann.getComDate(),
                    ann.getType(),
                    ann.getRate()
            );

            DividendAnnouncement existing = target.get(key);
            if (existing == null) {
                target.put(key, ann);
            } else {
                // Se o novo anúncio possuir payment_date e o anterior não, ou se for mais recente
                if (existing.getPaymentDate() == null && ann.getPaymentDate() != null) {
                    target.put(key, ann);
                }
            }
        }
    }

    @Override
    public List<RelevantFact> crawlRelevantFacts(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Iniciando coleta composta de fatos relevantes para {} tickers", tickers.size());
        if (cvmRelevantFactCrawler == null) {
            return Collections.emptyList();
        }

        try {
            return cvmRelevantFactCrawler.crawlRelevantFacts(tickers);
        } catch (Exception e) {
            log.warn("Erro ao coletar fatos relevantes na CVM: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
