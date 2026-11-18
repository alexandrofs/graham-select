package afsdigital.grahamselect.common.openfinance.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenFinanceSyncLog {
    private UUID id;
    private UUID connectionId;
    private String userId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private OpenFinanceSyncType syncType;
    private int positionsFetched;
    private int transactionsFetched;
    private int tradesCreated;
    private int tradesDuplicated;
    private OpenFinanceSyncStatus status;
    private String errorDetails;
    private LocalDateTime createdAt;
}
