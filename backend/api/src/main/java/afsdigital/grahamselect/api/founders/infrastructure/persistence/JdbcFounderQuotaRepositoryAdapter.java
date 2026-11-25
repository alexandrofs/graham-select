package afsdigital.grahamselect.api.founders.infrastructure.persistence;

import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.entities.FounderQuotaEntity;
import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.repository.FounderQuotaJpaRepository;
import afsdigital.grahamselect.common.founders.application.port.FounderQuotaRepositoryPort;
import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcFounderQuotaRepositoryAdapter implements FounderQuotaRepositoryPort {

    public static final long SINGLETON_QUOTA_ID = 1L;

    private static final String ATOMIC_CLAIM_SQL = """
            UPDATE tb_founder_quota
            SET founder_slots_remaining = founder_slots_remaining - 1,
                updated_at = :now
            WHERE id = :id AND founder_slots_remaining > 0
            """;

    private static final String ATOMIC_RECONCILE_SQL = """
            UPDATE tb_founder_quota
            SET founder_slots_remaining = CASE
                    WHEN founder_slots_remaining + (:newTotal - total_slots) < 0 THEN 0
                    ELSE founder_slots_remaining + (:newTotal - total_slots)
                END,
                total_slots = :newTotal,
                updated_at = :now
            WHERE id = :id AND total_slots <> :newTotal
            """;

    private final FounderQuotaJpaRepository jpaRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${app.founders.total-slots:250}")
    private int configuredTotalSlots;

    @Override
    @Transactional(readOnly = true)
    public FounderSlotsStatus getStatus() {
        int safeTotalSlots = Math.max(0, configuredTotalSlots);
        return jpaRepository.findById(SINGLETON_QUOTA_ID)
                .map(entity -> new FounderSlotsStatus(entity.getTotalSlots(), entity.getFounderSlotsRemaining()))
                .orElseGet(() -> new FounderSlotsStatus(safeTotalSlots, safeTotalSlots));
    }

    @Override
    @Transactional
    public boolean tryClaimFounderSlot(LocalDateTime nowUtc) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", SINGLETON_QUOTA_ID)
                .addValue("now", nowUtc);

        int affectedRows = jdbcTemplate.update(ATOMIC_CLAIM_SQL, params);
        return affectedRows == 1;
    }

    @Transactional
    public void reconcileConfiguredTotalSlots() {
        int safeTotalSlots = Math.max(0, configuredTotalSlots);
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", SINGLETON_QUOTA_ID)
                .addValue("newTotal", safeTotalSlots)
                .addValue("now", nowUtc);

        int updatedRows = jdbcTemplate.update(ATOMIC_RECONCILE_SQL, params);
        if (updatedRows > 0) {
            log.info("Reconciled founder quota atomically to totalSlots={}", safeTotalSlots);
            return;
        }

        if (!jpaRepository.existsById(SINGLETON_QUOTA_ID)) {
            try {
                FounderQuotaEntity created = FounderQuotaEntity.builder()
                        .id(SINGLETON_QUOTA_ID)
                        .totalSlots(safeTotalSlots)
                        .founderSlotsRemaining(safeTotalSlots)
                        .updatedAt(nowUtc)
                        .build();
                jpaRepository.saveAndFlush(created);
                log.info("Initialized founder quota singleton with totalSlots={}", safeTotalSlots);
            } catch (DataIntegrityViolationException concurrentInsert) {
                jdbcTemplate.update(ATOMIC_RECONCILE_SQL, params);
            }
        }
    }
}
