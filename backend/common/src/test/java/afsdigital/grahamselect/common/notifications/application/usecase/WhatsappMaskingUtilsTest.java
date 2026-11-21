package afsdigital.grahamselect.common.notifications.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappMaskingUtilsTest {

    @Test
    void shouldMaskShortAndStandardE164NumbersWithAtLeastFourMaskedChars() {
        assertThat(WhatsappMaskingUtils.maskPhoneNumber("+1234567890")).isEqualTo("+12****7890");
        assertThat(WhatsappMaskingUtils.maskPhoneNumber("+5511999998888")).isEqualTo("+551****8888");
        assertThat(WhatsappMaskingUtils.maskPhoneNumber("123456")).isEqualTo("****");
        assertThat(WhatsappMaskingUtils.maskPhoneNumber(null)).isEqualTo("****");
    }

    @Test
    void shouldBoundLongDedupKeysToMaxLength() {
        String longKey = "rebalance:" + "a".repeat(300) + ":2026-11-20";
        String bounded = WhatsappMaskingUtils.boundDedupKey(longKey, 255);

        assertThat(bounded).hasSize(255);
        assertThat(bounded).isEqualTo(WhatsappMaskingUtils.boundDedupKey(longKey, 255));
    }
}
