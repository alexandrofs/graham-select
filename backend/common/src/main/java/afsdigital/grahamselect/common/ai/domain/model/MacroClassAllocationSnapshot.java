package afsdigital.grahamselect.common.ai.domain.model;

import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Percentage-only allocation summary for a single {@link MacroClass}.
 */
public record MacroClassAllocationSnapshot(
        MacroClass macroClass,
        BigDecimal currentPercent,
        BigDecimal targetPercent,
        BigDecimal deviationPercent
) {
    public MacroClassAllocationSnapshot {
        Objects.requireNonNull(macroClass, "macroClass must not be null");
        Objects.requireNonNull(currentPercent, "currentPercent must not be null");
        Objects.requireNonNull(targetPercent, "targetPercent must not be null");
        Objects.requireNonNull(deviationPercent, "deviationPercent must not be null");
    }
}
