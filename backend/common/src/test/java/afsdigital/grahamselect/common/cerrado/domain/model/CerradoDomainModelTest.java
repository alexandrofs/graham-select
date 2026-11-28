package afsdigital.grahamselect.common.cerrado.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CerradoDomainModelTest {

    @Test
    @DisplayName("CerradoPreferences should apply defaults and preserve BigDecimal precision")
    void shouldCreatePreferencesWithDefaultsAndExactDecimals() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(42L)
                .targetAcoesBr(new BigDecimal("35.50"))
                .targetFiis(new BigDecimal("25.00"))
                .targetRendaFixa(new BigDecimal("24.50"))
                .targetInternacional(new BigDecimal("14.00"))
                .targetCripto(new BigDecimal("1.00"))
                .build();

        assertThat(prefs.getUserId()).isEqualTo(42L);
        assertThat(prefs.getTargetAcoesBr()).isEqualByComparingTo("35.50");
        assertThat(prefs.getTargetFiis()).isEqualByComparingTo("25.00");
        assertThat(prefs.getTargetRendaFixa()).isEqualByComparingTo("24.50");
        assertThat(prefs.getTargetInternacional()).isEqualByComparingTo("14.00");
        assertThat(prefs.getTargetCripto()).isEqualByComparingTo("1.00");
        assertThat(prefs.getGrahamMultiplier()).isEqualByComparingTo("1.30");
        assertThat(prefs.isGrahamFilterEnabled()).isTrue();
    }

    @Test
    @DisplayName("CerradoPreferences should reject null userId or null BigDecimal targets")
    void shouldRejectNullFieldsInPreferences() {
        assertThatThrownBy(() -> CerradoPreferences.builder()
                .userId(null)
                .targetAcoesBr(BigDecimal.TEN)
                .targetFiis(BigDecimal.TEN)
                .targetRendaFixa(BigDecimal.TEN)
                .targetInternacional(BigDecimal.TEN)
                .targetCripto(BigDecimal.TEN)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(null)
                .targetFiis(BigDecimal.TEN)
                .targetRendaFixa(BigDecimal.TEN)
                .targetInternacional(BigDecimal.TEN)
                .targetCripto(BigDecimal.TEN)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CerradoAssetGrade should apply default qualityGrade=6 and quarantine=false")
    void shouldCreateAssetGradeWithDefaults() {
        CerradoAssetGrade grade = CerradoAssetGrade.builder()
                .userId(7L)
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .build();

        assertThat(grade.getQualityGrade()).isEqualTo(6);
        assertThat(grade.isQuarantineCorporateEvent()).isFalse();
        assertThat(grade.getCustomRiskCeiling()).isNull();
    }

    @Test
    @DisplayName("CerradoAssetGrade should reject qualityGrade outside 1..10")
    void shouldRejectInvalidQualityGrade() {
        assertThatThrownBy(() -> CerradoAssetGrade.builder()
                .userId(7L)
                .ticker("PETR4")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CerradoAssetGrade.builder()
                .userId(7L)
                .ticker("PETR4")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(11)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
