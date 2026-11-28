package afsdigital.grahamselect.common.cerrado.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado consolidado e imutavel do calculo de alocacao do Diagrama do Cerrado.
 */
@Builder
public record CerradoAllocationResult(
        BigDecimal grahamMultiplier,
        boolean grahamFilterEnabled,
        Map<MacroClass, CerradoMacroClassSummary> macroClasses,
        List<CerradoAssetAllocation> allocations,
        BigDecimal totalAllocatedPct,
        BigDecimal totalUnallocatedResidualPct
) {

    public CerradoAllocationResult {
        if (macroClasses == null || macroClasses.isEmpty()) {
            macroClasses = Collections.emptyMap();
        } else {
            EnumMap<MacroClass, CerradoMacroClassSummary> ordered = new EnumMap<>(MacroClass.class);
            ordered.putAll(macroClasses);
            macroClasses = Collections.unmodifiableMap(ordered);
        }
        allocations = allocations == null ? List.of() : List.copyOf(allocations);
    }
}
