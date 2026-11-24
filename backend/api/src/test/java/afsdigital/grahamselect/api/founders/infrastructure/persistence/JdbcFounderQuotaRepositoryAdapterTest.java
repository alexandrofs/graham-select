package afsdigital.grahamselect.api.founders.infrastructure.persistence;

import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.entities.FounderQuotaEntity;
import afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.repository.FounderQuotaJpaRepository;
import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcFounderQuotaRepositoryAdapterTest {

    @Mock
    private FounderQuotaJpaRepository jpaRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private JdbcFounderQuotaRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JdbcFounderQuotaRepositoryAdapter(jpaRepository, jdbcTemplate);
        ReflectionTestUtils.setField(adapter, "configuredTotalSlots", 250);
    }

    @Test
    void getStatusShouldClampNegativeConfiguredTotalSlotsInFallback() {
        ReflectionTestUtils.setField(adapter, "configuredTotalSlots", -10);
        when(jpaRepository.findById(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID))
                .thenReturn(Optional.empty());

        FounderSlotsStatus status = adapter.getStatus();

        assertThat(status.totalSlots()).isZero();
        assertThat(status.remainingSlots()).isZero();
        assertThat(status.isLoteActive()).isFalse();
    }

    @Test
    void tryClaimFounderSlotShouldPassLocalDateTimeDirectly() {
        LocalDateTime nowUtc = LocalDateTime.of(2026, 11, 24, 18, 0, 0);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        boolean claimed = adapter.tryClaimFounderSlot(nowUtc);

        assertThat(claimed).isTrue();
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(anyString(), captor.capture());
        assertThat(captor.getValue().getValue("now")).isEqualTo(nowUtc);
    }

    @Test
    void reconcileShouldInitializeRowWhenMissing() {
        ReflectionTestUtils.setField(adapter, "configuredTotalSlots", 250);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);
        when(jpaRepository.existsById(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID)).thenReturn(false);

        adapter.reconcileConfiguredTotalSlots();

        ArgumentCaptor<FounderQuotaEntity> entityCaptor = ArgumentCaptor.forClass(FounderQuotaEntity.class);
        verify(jpaRepository).saveAndFlush(entityCaptor.capture());
        FounderQuotaEntity saved = entityCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(JdbcFounderQuotaRepositoryAdapter.SINGLETON_QUOTA_ID);
        assertThat(saved.getTotalSlots()).isEqualTo(250);
        assertThat(saved.getFounderSlotsRemaining()).isEqualTo(250);
        assertThat(saved.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1));
    }

    @Test
    void reconcileShouldExecuteAtomicUpdateWithConfiguredTotalSlots() {
        ReflectionTestUtils.setField(adapter, "configuredTotalSlots", 300);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        adapter.reconcileConfiguredTotalSlots();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), paramsCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("CASE");
        assertThat(sqlCaptor.getValue()).contains("founder_slots_remaining + (:newTotal - total_slots) < 0 THEN 0");
        assertThat(paramsCaptor.getValue().getValue("newTotal")).isEqualTo(300);
        verify(jpaRepository, never()).saveAndFlush(any());
    }
}
