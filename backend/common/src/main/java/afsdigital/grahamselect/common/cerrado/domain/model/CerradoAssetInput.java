package afsdigital.grahamselect.common.cerrado.domain.model;

import afsdigital.grahamselect.common.cerrado.domain.exceptions.CerradoValidationException;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Dados de entrada de um ativo para calculo de alocacao no Diagrama do Cerrado.
 */
@Builder
public record CerradoAssetInput(
        String ticker,
        MacroClass macroClass,
        CerradoSubClass subClass,
        int qualityGrade,
        BigDecimal customRiskCeiling,
        boolean quarantineCorporateEvent,
        boolean isDiversifiedBroadEtf
) {

    /**
     * Converte uma entidade de nota de qualidade persistida em entrada do motor de alocacao.
     */
    public static CerradoAssetInput fromGrade(CerradoAssetGrade grade, boolean isDiversifiedBroadEtf) {
        if (grade == null) {
            throw new CerradoValidationException("CerradoAssetGrade nao pode ser nulo.");
        }
        return new CerradoAssetInput(
                grade.getTicker(),
                grade.getMacroClass(),
                grade.getSubClass(),
                grade.getQualityGrade(),
                grade.getCustomRiskCeiling(),
                grade.isQuarantineCorporateEvent(),
                isDiversifiedBroadEtf
        );
    }
}
