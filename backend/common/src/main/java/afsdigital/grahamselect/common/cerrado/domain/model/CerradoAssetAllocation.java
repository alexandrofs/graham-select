package afsdigital.grahamselect.common.cerrado.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Resultado calculado de alocacao para um ativo individual no Diagrama do Cerrado.
 */
@Builder
public record CerradoAssetAllocation(
        String ticker,
        MacroClass macroClass,
        CerradoSubClass subClass,
        int qualityGrade,
        boolean quarantined,
        boolean quarantineCorporateEvent,
        boolean ceilingExempt,
        boolean ceilingApplied,
        BigDecimal riskCeilingPct,
        BigDecimal grossTargetPct,
        BigDecimal effectiveTargetPct
) {
}
