package afsdigital.grahamselect.common.cerrado.domain.model;

import java.math.BigDecimal;

/**
 * Perfil oficial de onboarding do Diagrama do Cerrado com metas das 5 Macro Classes.
 */
public record CerradoTemplate(
        String code,
        String name,
        String description,
        BigDecimal targetAcoesBr,
        BigDecimal targetFiis,
        BigDecimal targetRendaFixa,
        BigDecimal targetInternacional,
        BigDecimal targetCripto
) {
}
