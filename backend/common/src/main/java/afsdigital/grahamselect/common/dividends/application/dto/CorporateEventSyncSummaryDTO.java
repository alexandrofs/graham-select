package afsdigital.grahamselect.common.dividends.application.dto;

import java.time.Instant;

public record CorporateEventSyncSummaryDTO(
        int tickersProcessed,
        int newDividends,
        int updatedDividends,
        int newFacts,
        Instant executionTime,
        String status
) {
}
