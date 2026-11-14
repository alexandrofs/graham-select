package afsdigital.grahamselect.common.tags.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TagDetailsDto(
        UUID id,
        String name,
        String color,
        List<String> tickers,
        int assetCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
