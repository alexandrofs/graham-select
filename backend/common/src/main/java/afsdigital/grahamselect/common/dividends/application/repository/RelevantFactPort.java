package afsdigital.grahamselect.common.dividends.application.repository;

import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;

import java.time.Instant;
import java.util.List;

public interface RelevantFactPort {

    List<RelevantFact> saveAll(List<RelevantFact> facts);

    RelevantFact save(RelevantFact fact);

    List<RelevantFact> findByTickersAndDateRange(
            List<String> tickers,
            Instant startDate,
            Instant endDate,
            int limit
    );

    boolean existsByTickerAndPublicationDate(String ticker, Instant publicationDate);
}
