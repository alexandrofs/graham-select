package afsdigital.grahamselect.common.openfinance.application.dto;

import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpenFinanceSyncResultDTO(
        UUID connectionId,
        OpenFinanceSyncStatus status,
        int positionsFetched,
        int transactionsFetched,
        int tradesCreated,
        int tradesDuplicated,
        String message,
        LocalDateTime syncedAt
) {}
