package afsdigital.grahamselect.api.tags.web;

import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        String color,
        List<String> tickers,
        int assetCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TagResponse from(TagDetailsDto dto) {
        return new TagResponse(
                dto.id(),
                dto.name(),
                dto.color(),
                dto.tickers(),
                dto.assetCount(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
