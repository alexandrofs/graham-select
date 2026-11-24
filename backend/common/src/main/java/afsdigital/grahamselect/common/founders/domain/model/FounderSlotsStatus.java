package afsdigital.grahamselect.common.founders.domain.model;

public record FounderSlotsStatus(
        int totalSlots,
        int remainingSlots,
        boolean isLoteActive
) {
    public FounderSlotsStatus(int totalSlots, int remainingSlots) {
        this(totalSlots, Math.max(0, remainingSlots), remainingSlots > 0);
    }
}
