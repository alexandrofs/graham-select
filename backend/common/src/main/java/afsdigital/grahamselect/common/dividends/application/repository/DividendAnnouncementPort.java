package afsdigital.grahamselect.common.dividends.application.repository;

import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DividendAnnouncementPort {

    List<DividendAnnouncement> saveAll(List<DividendAnnouncement> announcements);

    DividendAnnouncement save(DividendAnnouncement announcement);

    List<DividendAnnouncement> findByTickersAndDateRange(
            List<String> tickers,
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

    Optional<DividendAnnouncement> findByTickerAndComDateAndTypeAndRate(
            String ticker,
            LocalDate comDate,
            CorporateEventType type,
            BigDecimal rate
    );

    boolean existsByTickerAndComDateAndTypeAndRate(
            String ticker,
            LocalDate comDate,
            CorporateEventType type,
            BigDecimal rate
    );
}
