package afsdigital.grahamselect.common.dividends.application.repository;

import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;

import java.util.List;

public interface DividendCrawlerPort {

    List<DividendAnnouncement> crawlDividends(List<String> tickers);
}
