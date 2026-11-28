package afsdigital.grahamselect.common.cerrado.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Preferencias estrategicas de alocacao por usuario no Diagrama do Cerrado.
 */
@Getter
@EqualsAndHashCode
@ToString
public class CerradoPreferences {

    public static final BigDecimal DEFAULT_GRAHAM_MULTIPLIER = new BigDecimal("1.30");

    private final Long id;
    private final Long userId;
    private final BigDecimal targetAcoesBr;
    private final BigDecimal targetFiis;
    private final BigDecimal targetRendaFixa;
    private final BigDecimal targetInternacional;
    private final BigDecimal targetCripto;
    private final BigDecimal grahamMultiplier;
    private final boolean grahamFilterEnabled;
    private final LocalDateTime updatedAt;

    @Builder
    public CerradoPreferences(
            Long id,
            Long userId,
            BigDecimal targetAcoesBr,
            BigDecimal targetFiis,
            BigDecimal targetRendaFixa,
            BigDecimal targetInternacional,
            BigDecimal targetCripto,
            BigDecimal grahamMultiplier,
            Boolean grahamFilterEnabled,
            LocalDateTime updatedAt
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        requireNonNullDecimal(targetAcoesBr, "targetAcoesBr");
        requireNonNullDecimal(targetFiis, "targetFiis");
        requireNonNullDecimal(targetRendaFixa, "targetRendaFixa");
        requireNonNullDecimal(targetInternacional, "targetInternacional");
        requireNonNullDecimal(targetCripto, "targetCripto");

        this.id = id;
        this.userId = userId;
        this.targetAcoesBr = targetAcoesBr;
        this.targetFiis = targetFiis;
        this.targetRendaFixa = targetRendaFixa;
        this.targetInternacional = targetInternacional;
        this.targetCripto = targetCripto;
        this.grahamMultiplier = grahamMultiplier != null ? grahamMultiplier : DEFAULT_GRAHAM_MULTIPLIER;
        this.grahamFilterEnabled = grahamFilterEnabled != null ? grahamFilterEnabled : true;
        this.updatedAt = updatedAt;
    }

    private static void requireNonNullDecimal(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
