package afsdigital.grahamselect.api.openfinance.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmTokenEncryptionAdapterTest {

    private static final String VALID_KEY_32_BYTES = "0123456789abcdef0123456789abcdef"; // 32 chars = 256 bits UTF-8
    private static final String ANOTHER_KEY_32_BYTES = "fedcba9876543210fedcba9876543210";

    private AesGcmTokenEncryptionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AesGcmTokenEncryptionAdapter(VALID_KEY_32_BYTES);
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt a plain text token")
    void testEncryptAndDecrypt() {
        String token = "access_token_super_secret_xyz1234567890";

        String encrypted = adapter.encrypt(token);

        assertThat(encrypted).isNotNull().isNotEqualTo(token);

        String decrypted = adapter.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(token);
    }

    @Test
    @DisplayName("Should produce different ciphertexts for the same plaintext due to random IV")
    void testRandomIvProducesDifferentCiphertexts() {
        String token = "my-same-token";

        String encrypted1 = adapter.encrypt(token);
        String encrypted2 = adapter.encrypt(token);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(adapter.decrypt(encrypted1)).isEqualTo(token);
        assertThat(adapter.decrypt(encrypted2)).isEqualTo(token);
    }

    @Test
    @DisplayName("Should return null or empty when encrypting or decrypting null/empty input")
    void testNullOrBlankHandling() {
        assertThat(adapter.encrypt(null)).isNull();
        assertThat(adapter.encrypt("")).isEmpty();
        assertThat(adapter.decrypt(null)).isNull();
        assertThat(adapter.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("Should fail when decrypting with a different secret key")
    void testDecryptWithDifferentKey() {
        String token = "confidential_refresh_token_abc";
        String encrypted = adapter.encrypt(token);

        AesGcmTokenEncryptionAdapter anotherAdapter = new AesGcmTokenEncryptionAdapter(ANOTHER_KEY_32_BYTES);

        assertThatThrownBy(() -> anotherAdapter.decrypt(encrypted))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should fail when decrypting tampered ciphertext")
    void testDecryptWithTamperedCiphertext() {
        String token = "confidential_token";
        String encrypted = adapter.encrypt(token);

        // Tamper with the base64 string
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThatThrownBy(() -> adapter.decrypt(tampered))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should fail instantiation when key is too short")
    void testInvalidKeyLength() {
        assertThatThrownBy(() -> new AesGcmTokenEncryptionAdapter("too_short_key"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
