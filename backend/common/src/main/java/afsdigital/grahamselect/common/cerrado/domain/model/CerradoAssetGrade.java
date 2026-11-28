package afsdigital.grahamselect.common.cerrado.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Classificacao e nota de qualidade de um ativo para um usuario no Diagrama do Cerrado.
 */
@Getter
@EqualsAndHashCode
@ToString
public class CerradoAssetGrade {

    public static final int DEFAULT_QUALITY_GRADE = 6;
    public static final int MIN_QUALITY_GRADE = 1;
    public static final int MAX_QUALITY_GRADE = 10;

    private final Long id;
    private final Long userId;
    private final String ticker;
    private final MacroClass macroClass;
    private final CerradoSubClass subClass;
    private final int qualityGrade;
    private final BigDecimal customRiskCeiling;
    private final boolean quarantineCorporateEvent;
    private final LocalDateTime updatedAt;

    @Builder
    public CerradoAssetGrade(
            Long id,
            Long userId,
            String ticker,
            MacroClass macroClass,
            CerradoSubClass subClass,
            Integer qualityGrade,
            BigDecimal customRiskCeiling,
            Boolean quarantineCorporateEvent,
            LocalDateTime updatedAt
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker must not be blank");
        }
        if (macroClass == null) {
            throw new IllegalArgumentException("macroClass must not be null");
        }
        if (subClass == null) {
            throw new IllegalArgumentException("subClass must not be null");
        }

        int resolvedGrade = qualityGrade != null ? qualityGrade : DEFAULT_QUALITY_GRADE;
        if (resolvedGrade < MIN_QUALITY_GRADE || resolvedGrade > MAX_QUALITY_GRADE) {
            throw new IllegalArgumentException("qualityGrade must be between 1 and 10");
        }

        this.id = id;
        this.userId = userId;
        this.ticker = ticker;
        this.macroClass = macroClass;
        this.subClass = subClass;
        this.qualityGrade = resolvedGrade;
        this.customRiskCeiling = customRiskCeiling;
        this.quarantineCorporateEvent = quarantineCorporateEvent != null ? quarantineCorporateEvent : false;
        this.updatedAt = updatedAt;
    }
}
