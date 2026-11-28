package afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.entities.DividendAnnouncementEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DividendAnnouncementJpaRepository extends JpaRepository<DividendAnnouncementEntity, UUID> {

    Optional<DividendAnnouncementEntity> findByTickerAndComDateAndTypeAndRate(
            String ticker,
            LocalDate comDate,
            String type,
            BigDecimal rate
    );

    boolean existsByTickerAndComDateAndTypeAndRate(
            String ticker,
            LocalDate comDate,
            String type,
            BigDecimal rate
    );

    @Query("""
            SELECT d FROM DividendAnnouncementEntity d
            WHERE (:tickersEmpty = true OR d.ticker IN :tickers)
              AND (:status IS NULL OR d.status = :status)
              AND (:startDate IS NULL OR d.paymentDate >= :startDate OR (d.paymentDate IS NULL AND d.comDate >= :startDate))
              AND (:endDate IS NULL OR d.paymentDate <= :endDate OR (d.paymentDate IS NULL AND d.comDate <= :endDate))
            ORDER BY COALESCE(d.paymentDate, d.comDate) ASC, d.ticker ASC
            """)
    List<DividendAnnouncementEntity> findSchedule(
            @Param("tickers") List<String> tickers,
            @Param("tickersEmpty") boolean tickersEmpty,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status,
            Pageable pageable
    );
}
