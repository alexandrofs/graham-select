package afsdigital.grahamselect.common.tags.application.dto;

import java.util.UUID;

public record TagSummaryDto(
        UUID id,
        String name,
        String color
) {
}
