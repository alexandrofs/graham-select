package afsdigital.grahamselect.api.tags.web;

import afsdigital.grahamselect.common.tags.application.dto.AssetTagsAssignmentDto;
import afsdigital.grahamselect.common.tags.application.dto.TagSummaryDto;

import java.util.List;
import java.util.UUID;

public record AssetTagsResponse(
        String ticker,
        List<TagSummaryResponse> tags
) {
    public record TagSummaryResponse(UUID id, String name, String color) {
        public static TagSummaryResponse from(TagSummaryDto dto) {
            return new TagSummaryResponse(dto.id(), dto.name(), dto.color());
        }
    }

    public static AssetTagsResponse from(AssetTagsAssignmentDto dto) {
        return new AssetTagsResponse(
                dto.ticker(),
                dto.tags().stream().map(TagSummaryResponse::from).toList()
        );
    }
}
