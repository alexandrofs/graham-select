package afsdigital.grahamselect.common.notifications.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushMaskingUtilsTest {

    @Test
    void shouldMaskEndpointSafely() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/fake-secret-token-12345";
        String masked = WebPushMaskingUtils.maskEndpoint(endpoint);

        assertThat(masked).isEqualTo("https://fcm.googleapis.com/.../****");
        assertThat(masked).doesNotContain("fake-secret-token");
    }

    @Test
    void shouldHandleNullOrBlankEndpoint() {
        assertThat(WebPushMaskingUtils.maskEndpoint(null)).isEqualTo("****");
        assertThat(WebPushMaskingUtils.maskEndpoint("   ")).isEqualTo("****");
    }

    @Test
    void shouldTruncateLongStrings() {
        assertThat(WebPushMaskingUtils.truncate("hello", 10)).isEqualTo("hello");
        assertThat(WebPushMaskingUtils.truncate("1234567890abcdef", 10)).isEqualTo("1234567...");
        assertThat(WebPushMaskingUtils.truncate(null, 10)).isEmpty();
    }

    @Test
    void shouldBoundDedupKeyPreservingHashWhenExceedingLength() {
        String originalKey = "a".repeat(300);
        String bounded = WebPushMaskingUtils.boundDedupKey(originalKey, 255);

        assertThat(bounded.length()).isLessThanOrEqualTo(255);
        assertThat(bounded).contains(":");
    }

    @Test
    void shouldComputeDeterministicEndpointHash() {
        String hash1 = WebPushMaskingUtils.endpointHash("user1", "https://push.com/endpoint");
        String hash2 = WebPushMaskingUtils.endpointHash("user1", "https://push.com/endpoint");
        String hashOther = WebPushMaskingUtils.endpointHash("user2", "https://push.com/endpoint");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
        assertThat(hash1).isNotEqualTo(hashOther);
    }
}
