package afsdigital.grahamselect.api.dividends.infrastructure.persistence;

import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.entities.RelevantFactEntity;
import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.repositories.RelevantFactJpaRepository;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactPort;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelevantFactJpaAdapter implements RelevantFactPort {

    private final RelevantFactJpaRepository repository;

    @Override
    @Transactional
    public List<RelevantFact> saveAll(List<RelevantFact> facts) {
        if (facts == null || facts.isEmpty()) {
            return List.of();
        }
        List<RelevantFact> saved = new ArrayList<>();
        for (RelevantFact fact : facts) {
            if (fact != null) {
                saved.add(save(fact));
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public RelevantFact save(RelevantFact fact) {
        if (fact == null) return null;
        if (fact.getPublicationDate() == null) {
            log.warn("Tentativa de salvar fato relevante sem data de publicação para ticker={}", fact.getTicker());
            return fact;
        }

        LocalDateTime pubDateUtc = LocalDateTime.ofInstant(fact.getPublicationDate(), ZoneOffset.UTC);

        if (repository.existsByTickerAndPublicationDate(fact.getTicker(), pubDateUtc)) {
            log.info("Fato relevante já existente para ticker={} e data={}. Ignorando duplicata.", fact.getTicker(), pubDateUtc);
            return fact;
        }

        LocalDateTime createdUtc = fact.getCreatedAt() != null
                ? LocalDateTime.ofInstant(fact.getCreatedAt(), ZoneOffset.UTC)
                : LocalDateTime.now(ZoneOffset.UTC);

        RelevantFactEntity entity = RelevantFactEntity.builder()
                .id(fact.getId())
                .ticker(fact.getTicker())
                .title(fact.getTitle())
                .summary(fact.getSummary())
                .sourceUrl(fact.getSourceUrl())
                .category(fact.getCategory())
                .publicationDate(pubDateUtc)
                .createdAt(createdUtc)
                .isNew(true)
                .build();

        RelevantFactEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelevantFact> findByTickersAndDateRange(
            List<String> tickers,
            Instant startDate,
            Instant endDate,
            int limit
    ) {
        boolean tickersEmpty = tickers == null || tickers.isEmpty();
        List<String> safeTickers = tickersEmpty ? List.of("") : tickers;
        LocalDateTime startUtc = startDate != null ? LocalDateTime.ofInstant(startDate, ZoneOffset.UTC) : null;
        LocalDateTime endUtc = endDate != null ? LocalDateTime.ofInstant(endDate, ZoneOffset.UTC) : null;
        int pageLimit = limit > 0 ? limit : 20;

        List<RelevantFactEntity> entities = repository.findRecentFacts(
                safeTickers,
                tickersEmpty,
                startUtc,
                endUtc,
                PageRequest.of(0, pageLimit)
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTickerAndPublicationDate(String ticker, Instant publicationDate) {
        if (ticker == null || publicationDate == null) return false;
        LocalDateTime pubDateUtc = LocalDateTime.ofInstant(publicationDate, ZoneOffset.UTC);
        return repository.existsByTickerAndPublicationDate(ticker, pubDateUtc);
    }

    private RelevantFact toDomain(RelevantFactEntity entity) {
        Instant pubInstant = entity.getPublicationDate() != null
                ? entity.getPublicationDate().toInstant(ZoneOffset.UTC)
                : null;
        Instant createdInstant = entity.getCreatedAt() != null
                ? entity.getCreatedAt().toInstant(ZoneOffset.UTC)
                : null;

        return RelevantFact.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .sourceUrl(entity.getSourceUrl())
                .category(entity.getCategory())
                .publicationDate(pubInstant)
                .createdAt(createdInstant)
                .build();
    }
}
