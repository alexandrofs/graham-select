package afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities;

import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncLog;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "open_finance_sync_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenFinanceSyncLogEntity implements Persistable<UUID> {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "connection_id", length = 36, nullable = false)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "sync_type", nullable = false, length = 30)
    private String syncType;

    @Column(name = "positions_fetched", nullable = false)
    private int positionsFetched;

    @Column(name = "transactions_fetched", nullable = false)
    private int transactionsFetched;

    @Column(name = "trades_created", nullable = false)
    private int tradesCreated;

    @Column(name = "trades_duplicated", nullable = false)
    private int tradesDuplicated;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public OpenFinanceSyncLog toDomain() {
        return OpenFinanceSyncLog.builder()
                .id(id)
                .connectionId(connectionId)
                .userId(userId)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .syncType(syncType != null ? OpenFinanceSyncType.valueOf(syncType) : null)
                .positionsFetched(positionsFetched)
                .transactionsFetched(transactionsFetched)
                .tradesCreated(tradesCreated)
                .tradesDuplicated(tradesDuplicated)
                .status(status != null ? OpenFinanceSyncStatus.valueOf(status) : null)
                .errorDetails(errorDetails)
                .createdAt(createdAt)
                .build();
    }

    public static OpenFinanceSyncLogEntity fromDomain(OpenFinanceSyncLog domain) {
        if (domain == null) {
            return null;
        }
        return OpenFinanceSyncLogEntity.builder()
                .id(domain.getId())
                .connectionId(domain.getConnectionId())
                .userId(domain.getUserId())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .syncType(domain.getSyncType() != null ? domain.getSyncType().name() : null)
                .positionsFetched(domain.getPositionsFetched())
                .transactionsFetched(domain.getTransactionsFetched())
                .tradesCreated(domain.getTradesCreated())
                .tradesDuplicated(domain.getTradesDuplicated())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .errorDetails(domain.getErrorDetails())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
