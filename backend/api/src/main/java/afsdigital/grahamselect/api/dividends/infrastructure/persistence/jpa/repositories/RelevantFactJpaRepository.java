package afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.entities.RelevantFactEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RelevantFactJpaRepository extends JpaRepository<RelevantFactEntity, UUID> {

    boolean existsByTickerAndPublicationDate(String ticker, LocalDateTime publicationDate);

    @Query("""
            SELECT r FROM RelevantFactEntity r
            WHERE (:tickersEmpty = true OR r.ticker IN :tickers)
              AND (:startDate IS NULL OR r.publicationDate >= :startDate)
              AND (:endDate IS NULL OR r.publicationDate <= :endDate)
            ORDER BY r.publicationDate DESC
            """)
    List<RelevantFactEntity> findRecentFacts(
            @Param("tickers") List<String> tickers,
            @Param("tickersEmpty") boolean tickersEmpty,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
