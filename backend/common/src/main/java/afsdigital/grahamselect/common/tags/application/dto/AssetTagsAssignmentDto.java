package afsdigital.grahamselect.common.tags.application.dto;

import java.util.List;

public record AssetTagsAssignmentDto(
        String ticker,
        List<TagSummaryDto> tags
) {
}
