package afsdigital.grahamselect.common.ai.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * LGPD-sanitized portfolio snapshot safe for external AI payload serialization.
 * Contains exclusively relative percentages, grades, canonical/anonymous tickers, and HHI index.
 */
public record SanitizedPortfolioSnapshot(
        List<SanitizedAssetPosition> positions,
        List<MacroClassAllocationSnapshot> macroClassAllocations,
        BigDecimal hhiIndex
) {
    public SanitizedPortfolioSnapshot {
        positions = positions == null ? List.of() : List.copyOf(positions);
        macroClassAllocations = macroClassAllocations == null
                ? List.of()
                : List.copyOf(macroClassAllocations);
        Objects.requireNonNull(hhiIndex, "hhiIndex must not be null");
    }
}
