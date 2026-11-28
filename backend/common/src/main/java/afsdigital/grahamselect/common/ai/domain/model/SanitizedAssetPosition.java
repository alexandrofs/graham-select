package afsdigital.grahamselect.common.ai.domain.model;

import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * LGPD-sanitized asset position. Contains zero PII, zero absolute quantities,
 * and zero BRL monetary values.
 */
public record SanitizedAssetPosition(
        String canonicalTicker,
        MacroClass macroClass,
        CerradoSubClass subClass,
        BigDecimal weightPercent,
        Integer qualityGrade,
        boolean quarantineCorporateEvent,
        BigDecimal priceToEarnings,
        BigDecimal priceToBook,
        BigDecimal dividendYieldPercent,
        BigDecimal grahamMarginPercent
) {
    public SanitizedAssetPosition {
        if (canonicalTicker == null || canonicalTicker.isBlank()) {
            throw new IllegalArgumentException("canonicalTicker must not be blank");
        }
        Objects.requireNonNull(macroClass, "macroClass must not be null");
        Objects.requireNonNull(weightPercent, "weightPercent must not be null");
    }
}
