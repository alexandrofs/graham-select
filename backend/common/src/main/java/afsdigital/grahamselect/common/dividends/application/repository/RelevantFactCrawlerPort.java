package afsdigital.grahamselect.common.dividends.application.repository;

import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;

import java.util.List;

public interface RelevantFactCrawlerPort {

    List<RelevantFact> crawlRelevantFacts(List<String> tickers);
}
