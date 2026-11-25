package afsdigital.grahamselect.api.founders.web.dto;

import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FounderSlotsResponseDto(
        @JsonProperty("totalSlots") int totalSlots,
        @JsonProperty("remainingSlots") int remainingSlots,
        @JsonProperty("isLoteActive") boolean isLoteActive
) {
    public static FounderSlotsResponseDto from(FounderSlotsStatus status) {
        return new FounderSlotsResponseDto(
                status.totalSlots(),
                status.remainingSlots(),
                status.isLoteActive()
        );
    }
}
