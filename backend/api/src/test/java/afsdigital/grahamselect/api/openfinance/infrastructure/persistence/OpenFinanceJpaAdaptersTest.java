package afsdigital.grahamselect.api.openfinance.infrastructure.persistence;

import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities.OpenFinanceConnectionEntity;
import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities.OpenFinanceSyncLogEntity;
import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.repositories.OpenFinanceConnectionJpaRepository;
import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.repositories.OpenFinanceSyncLogJpaRepository;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncLog;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenFinanceJpaAdaptersTest {

    @Mock
    private OpenFinanceConnectionJpaRepository connectionJpaRepository;

    @Mock
    private OpenFinanceSyncLogJpaRepository syncLogJpaRepository;

    private OpenFinanceConnectionJpaAdapter connectionAdapter;
    private OpenFinanceSyncLogJpaAdapter syncLogAdapter;

    @BeforeEach
    void setUp() {
        connectionAdapter = new OpenFinanceConnectionJpaAdapter(connectionJpaRepository);
        syncLogAdapter = new OpenFinanceSyncLogJpaAdapter(syncLogJpaRepository);
    }

    @Test
    @DisplayName("Should save and map OpenFinanceConnection entity correctly")
    void testSaveConnection() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(id)
                .userId("user-123")
                .institutionId("XP_INVESTIMENTOS")
                .institutionName("XP Investimentos")
                .status(OpenFinanceConnectionStatus.PENDING_CONSENT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        OpenFinanceConnectionEntity entity = OpenFinanceConnectionEntity.fromDomain(connection);

        when(connectionJpaRepository.existsById(id)).thenReturn(false);
        when(connectionJpaRepository.save(any(OpenFinanceConnectionEntity.class))).thenReturn(entity);

        OpenFinanceConnection saved = connectionAdapter.save(connection);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getStatus()).isEqualTo(OpenFinanceConnectionStatus.PENDING_CONSENT);
        verify(connectionJpaRepository).save(any(OpenFinanceConnectionEntity.class));
    }

    @Test
    @DisplayName("Should find connection by userId, institutionId, profileId")
    void testFindByUserIdAndInstitutionAndProfile() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        OpenFinanceConnectionEntity entity = OpenFinanceConnectionEntity.builder()
                .id(id)
                .userId("user-123")
                .profileId(profileId)
                .institutionId("BTG_PACTUAL")
                .institutionName("BTG Pactual")
                .status(OpenFinanceConnectionStatus.ACTIVE.name())
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        when(connectionJpaRepository.findByUserIdAndInstitutionIdAndProfileId("user-123", "BTG_PACTUAL", profileId))
                .thenReturn(Optional.of(entity));

        Optional<OpenFinanceConnection> result = connectionAdapter.findByUserIdAndInstitutionIdAndProfileId("user-123", "BTG_PACTUAL", profileId);

        assertThat(result).isPresent();
        assertThat(result.get().getInstitutionId()).isEqualTo("BTG_PACTUAL");
        assertThat(result.get().getStatus()).isEqualTo(OpenFinanceConnectionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find active connections")
    void testFindActiveConnections() {
        OpenFinanceConnectionEntity entity = OpenFinanceConnectionEntity.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .institutionId("NUBANK")
                .institutionName("Nubank")
                .status(OpenFinanceConnectionStatus.ACTIVE.name())
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        when(connectionJpaRepository.findActiveNotExpired(eq(OpenFinanceConnectionStatus.ACTIVE.name()), any(LocalDateTime.class)))
                .thenReturn(List.of(entity));

        List<OpenFinanceConnection> active = connectionAdapter.findActiveConnections();

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getInstitutionId()).isEqualTo("NUBANK");
    }

    @Test
    @DisplayName("Should save and map OpenFinanceSyncLog entity correctly")
    void testSaveSyncLog() {
        UUID id = UUID.randomUUID();
        UUID connId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        OpenFinanceSyncLog log = OpenFinanceSyncLog.builder()
                .id(id)
                .connectionId(connId)
                .userId("user-123")
                .startedAt(now)
                .completedAt(now.plusSeconds(5))
                .syncType(OpenFinanceSyncType.MANUAL)
                .positionsFetched(2)
                .transactionsFetched(10)
                .tradesCreated(8)
                .tradesDuplicated(2)
                .status(OpenFinanceSyncStatus.SUCCESS)
                .createdAt(now)
                .build();

        OpenFinanceSyncLogEntity entity = OpenFinanceSyncLogEntity.fromDomain(log);

        when(syncLogJpaRepository.existsById(id)).thenReturn(false);
        when(syncLogJpaRepository.save(any(OpenFinanceSyncLogEntity.class))).thenReturn(entity);

        OpenFinanceSyncLog saved = syncLogAdapter.save(log);

        assertThat(saved).isNotNull();
        assertThat(saved.getTradesCreated()).isEqualTo(8);
        assertThat(saved.getTradesDuplicated()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(OpenFinanceSyncStatus.SUCCESS);
    }
}
