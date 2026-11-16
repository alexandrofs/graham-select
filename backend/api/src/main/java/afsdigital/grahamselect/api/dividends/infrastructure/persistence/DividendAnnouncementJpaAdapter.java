package afsdigital.grahamselect.api.dividends.infrastructure.persistence;

import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.entities.DividendAnnouncementEntity;
import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.repositories.DividendAnnouncementJpaRepository;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DividendAnnouncementJpaAdapter implements DividendAnnouncementPort {

    private final DividendAnnouncementJpaRepository repository;

    @Override
    @Transactional
    public List<DividendAnnouncement> saveAll(List<DividendAnnouncement> announcements) {
        if (announcements == null || announcements.isEmpty()) {
            return List.of();
        }
        List<DividendAnnouncement> saved = new ArrayList<>();
        for (DividendAnnouncement announcement : announcements) {
            saved.add(save(announcement));
        }
        return saved;
    }

    @Override
    @Transactional
    public DividendAnnouncement save(DividendAnnouncement announcement) {
        Optional<DividendAnnouncementEntity> existingOpt = repository.findByTickerAndComDateAndTypeAndRate(
                announcement.getTicker(),
                announcement.getComDate(),
                announcement.getType().name(),
                announcement.getRate()
        );

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        DividendAnnouncementEntity entityToSave;
        if (existingOpt.isPresent()) {
            DividendAnnouncementEntity existing = existingOpt.get();
            existing.setPaymentDate(announcement.getPaymentDate() != null ? announcement.getPaymentDate() : existing.getPaymentDate());
            existing.setExDate(announcement.getExDate() != null ? announcement.getExDate() : existing.getExDate());
            existing.setApprovedDate(announcement.getApprovedDate() != null ? announcement.getApprovedDate() : existing.getApprovedDate());
            existing.setNetValue(announcement.getNetValue() != null ? announcement.getNetValue() : existing.getNetValue());
            existing.setStatus(announcement.getStatus() != null ? announcement.getStatus() : existing.getStatus());
            existing.setSource(announcement.getSource() != null ? announcement.getSource() : existing.getSource());
            existing.setUpdatedAt(nowUtc);
            entityToSave = existing;
        } else {
            LocalDateTime createdUtc = announcement.getCreatedAt() != null
                    ? LocalDateTime.ofInstant(announcement.getCreatedAt(), ZoneOffset.UTC)
                    : nowUtc;
            LocalDateTime updatedUtc = announcement.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(announcement.getUpdatedAt(), ZoneOffset.UTC)
                    : nowUtc;

            entityToSave = DividendAnnouncementEntity.builder()
                    .id(announcement.getId())
                    .ticker(announcement.getTicker())
                    .type(announcement.getType().name())
                    .rate(announcement.getRate())
                    .netValue(announcement.getNetValue())
                    .approvedDate(announcement.getApprovedDate())
                    .comDate(announcement.getComDate())
                    .exDate(announcement.getExDate())
                    .paymentDate(announcement.getPaymentDate())
                    .source(announcement.getSource())
                    .status(announcement.getStatus())
                    .createdAt(createdUtc)
                    .updatedAt(updatedUtc)
                    .isNew(true)
                    .build();
        }

        DividendAnnouncementEntity saved = repository.save(entityToSave);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DividendAnnouncement> findByTickersAndDateRange(
            List<String> tickers,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        boolean tickersEmpty = tickers == null || tickers.isEmpty();
        List<DividendAnnouncementEntity> entities = repository.findSchedule(
                tickers,
                tickersEmpty,
                startDate,
                endDate,
                status
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DividendAnnouncement> findByTickerAndComDateAndTypeAndRate(
            String ticker,
            LocalDate comDate,
            CorporateEventType type,
            BigDecimal rate
    ) {
        return repository.findByTickerAndComDateAndTypeAndRate(ticker, comDate, type.name(), rate)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTickerAndComDateAndTypeAndRate(
            String ticker,
            LocalDate comDate,
            CorporateEventType type,
            BigDecimal rate
    ) {
        return repository.existsByTickerAndComDateAndTypeAndRate(ticker, comDate, type.name(), rate);
    }

    private DividendAnnouncement toDomain(DividendAnnouncementEntity entity) {
        Instant createdInstant = entity.getCreatedAt() != null
                ? entity.getCreatedAt().toInstant(ZoneOffset.UTC)
                : null;
        Instant updatedInstant = entity.getUpdatedAt() != null
                ? entity.getUpdatedAt().toInstant(ZoneOffset.UTC)
                : null;

        return DividendAnnouncement.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .type(CorporateEventType.fromString(entity.getType()))
                .rate(entity.getRate())
                .netValue(entity.getNetValue())
                .approvedDate(entity.getApprovedDate())
                .comDate(entity.getComDate())
                .exDate(entity.getExDate())
                .paymentDate(entity.getPaymentDate())
                .source(entity.getSource())
                .status(entity.getStatus())
                .createdAt(createdInstant)
                .updatedAt(updatedInstant)
                .build();
    }
}
