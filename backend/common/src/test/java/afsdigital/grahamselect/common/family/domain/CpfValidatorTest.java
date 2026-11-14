package afsdigital.grahamselect.common.family.domain;

import afsdigital.grahamselect.common.family.application.service.exceptions.InvalidCpfException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfValidatorTest {

    @Test
    void shouldValidateAndSanitizeFormattedValidCpf() {
        String result = CpfValidator.validateAndSanitize("529.982.247-25");
        assertThat(result).isEqualTo("52998224725");
        assertThat(CpfValidator.isValid("52998224725")).isTrue();
    }

    @Test
    void shouldRejectRepeatedDigitsAndInvalidCheckDigits() {
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("00000000000")).isFalse();
        assertThat(CpfValidator.isValid("52998224726")).isFalse();
        assertThat(CpfValidator.isValid("12345")).isFalse();
        assertThat(CpfValidator.isValid(null)).isFalse();

        assertThatThrownBy(() -> CpfValidator.validateAndSanitize("111.111.111-11"))
                .isInstanceOf(InvalidCpfException.class);
    }

    @Test
    void shouldMaskCpfFollowingLgpdPattern() {
        assertThat(CpfValidator.mask("52998224725")).isEqualTo("***.982.247-**");
        assertThat(CpfValidator.mask("invalid")).isEqualTo("***.***.***-**");
    }
}

