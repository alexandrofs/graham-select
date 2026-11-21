package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushCryptoUtilsTest {

    @Test
    void shouldEncodeAndDecodeKeysAndEncryptPayloadSuccessfully() throws Exception {
        KeyPair subscriberKeyPair = WebPushCryptoUtils.generateKeyPair();
        byte[] subscriberPublicBytes = WebPushCryptoUtils.encodePublicKey((ECPublicKey) subscriberKeyPair.getPublic());
        byte[] subscriberAuth = new byte[16];
        for (int i = 0; i < 16; i++) {
            subscriberAuth[i] = (byte) (i + 1);
        }

        byte[] payload = "{\"title\":\"Hello\",\"body\":\"World\"}".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = WebPushCryptoUtils.encryptPayload(payload, subscriberPublicBytes, subscriberAuth);

        // Header: 16 (salt) + 4 (rs) + 1 (idlen) + 65 (asKey) + ciphertext
        assertThat(encrypted.length).isGreaterThan(16 + 4 + 1 + 65);
        assertThat(encrypted[20]).isEqualTo((byte) 65); // idlen
        assertThat(encrypted[21]).isEqualTo((byte) 0x04); // asPublicKey starts with 0x04
    }

    @Test
    void shouldCreateValidVapidJwtWithEs256Signature() throws Exception {
        KeyPair serverKeyPair = WebPushCryptoUtils.generateKeyPair();
        ECPrivateKey privateKey = (ECPrivateKey) serverKeyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) serverKeyPair.getPublic();

        String origin = "https://fcm.googleapis.com";
        String subject = "mailto:test@example.com";

        String jwt = WebPushCryptoUtils.createVapidJwt(origin, subject, privateKey);

        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        String header = new String(WebPushCryptoUtils.decodeBase64Url(parts[0]), StandardCharsets.UTF_8);
        String claims = new String(WebPushCryptoUtils.decodeBase64Url(parts[1]), StandardCharsets.UTF_8);

        assertThat(header).contains("\"alg\":\"ES256\"");
        assertThat(claims).contains("\"aud\":\"https://fcm.googleapis.com\"");
        assertThat(claims).contains("\"sub\":\"mailto:test@example.com\"");

        byte[] sigBytes = WebPushCryptoUtils.decodeBase64Url(parts[2]);
        assertThat(sigBytes).hasSize(64); // IEEE P1363 raw signature format (32 r + 32 s)

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(publicKey);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(sigBytes)).isTrue();
    }

    @Test
    void shouldExtractOriginCorrectly() {
        assertThat(WebPushCryptoUtils.extractOrigin("https://fcm.googleapis.com/fcm/send/123"))
                .isEqualTo("https://fcm.googleapis.com");
        assertThat(WebPushCryptoUtils.extractOrigin("http://localhost:8080/push/v1"))
                .isEqualTo("http://localhost:8080");
    }
}
