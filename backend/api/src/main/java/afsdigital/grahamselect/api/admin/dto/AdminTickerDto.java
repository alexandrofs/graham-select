package afsdigital.grahamselect.api.admin.dto;

import java.time.OffsetDateTime;

public record AdminTickerDto(
        String id,
        String ticker,
        String name,
        String assetClass,
        String sector,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
