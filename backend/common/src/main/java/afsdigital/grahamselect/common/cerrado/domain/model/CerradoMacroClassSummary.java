package afsdigital.grahamselect.common.cerrado.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo consolidado de alocacao para uma Macro Classe do Diagrama do Cerrado.
 */
@Builder
public record CerradoMacroClassSummary(
        MacroClass macroClass,
        BigDecimal targetPct,
        BigDecimal allocatedPct,
        BigDecimal unallocatedResidualPct,
        int totalAssets,
        int eligibleAssets,
        int quarantinedAssets,
        List<CerradoAssetAllocation> assets
) {

    public CerradoMacroClassSummary {
        assets = assets == null ? List.of() : List.copyOf(assets);
    }
}
