package afsdigital.grahamselect.common.tags.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagInputValidatorTest {

    @Test
    void shouldTrimAndValidateTagName() {
        assertThat(TagInputValidator.validateAndNormalizeName("  Aposentadoria  "))
                .isEqualTo("Aposentadoria");
        assertThat(TagInputValidator.validateAndNormalizeName("A".repeat(50)))
                .hasSize(50);
    }

    @Test
    void shouldRejectInvalidTagNames() {
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName("A".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName("Longo\nPrazo"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName("Longo\tPrazo"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName("Tag\u0000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeName("\u2003\u2002"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldStripUnicodeWhitespaceInNameAndColor() {
        assertThat(TagInputValidator.validateAndNormalizeName("\u2003Dividendos\u2003"))
                .isEqualTo("Dividendos");
        assertThat(TagInputValidator.validateAndNormalizeColor("\u2003#10b981\u2003"))
                .isEqualTo("#10B981");
    }

    @Test
    void shouldNormalizeNameKeyWithRootLocale() {
        assertThat(TagInputValidator.toNormalizedNameKey("  Longo Prazo "))
                .isEqualTo("longo prazo");
    }

    @Test
    void shouldValidateAndNormalizeColor() {
        assertThat(TagInputValidator.validateAndNormalizeColor(null))
                .isEqualTo(TagInputValidator.DEFAULT_COLOR);
        assertThat(TagInputValidator.validateAndNormalizeColor("   "))
                .isEqualTo(TagInputValidator.DEFAULT_COLOR);
        assertThat(TagInputValidator.validateAndNormalizeColor(" #10b981 "))
                .isEqualTo("#10B981");

        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeColor("10B981"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeColor("#FFF"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeColor("#GGGGGG"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateAndNormalizeStockAndFixedIncomeTickers() {
        assertThat(TagInputValidator.validateAndNormalizeTicker(" petr4 "))
                .isEqualTo("PETR4");
        assertThat(TagInputValidator.validateAndNormalizeTicker("cdb banco x"))
                .isEqualTo("CDB BANCO X");
        assertThat(TagInputValidator.validateAndNormalizeTicker("lci-2026"))
                .isEqualTo("LCI-2026");
        assertThat(TagInputValidator.validateAndNormalizeTicker("TESOURO IPCA+ 2035"))
                .isEqualTo("TESOURO IPCA+ 2035");
    }

    @Test
    void shouldRejectInvalidOrUnsafeTickers() {
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("../PETR4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("PETR/4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("PETR\\4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("PETR..4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("PETR4\n"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("-PETR4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagInputValidator.validateAndNormalizeTicker("A".repeat(21)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
