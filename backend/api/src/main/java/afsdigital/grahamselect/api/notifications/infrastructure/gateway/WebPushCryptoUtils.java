package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

public final class WebPushCryptoUtils {

    private static final String CURVE_NAME = "secp256r1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final byte[] AES128GCM_INFO = "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NONCE_INFO = "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBPUSH_INFO_PREFIX = "WebPush: info\0".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private WebPushCryptoUtils() {
    }

    public static byte[] decodeBase64Url(String input) {
        if (input == null) {
            return new byte[0];
        }
        return Base64.getUrlDecoder().decode(input.trim());
    }

    public static String encodeBase64Url(byte[] input) {
        if (input == null) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    public static ECParameterSpec getEcParameterSpec() {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec(CURVE_NAME));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get EC parameter spec for " + CURVE_NAME, e);
        }
    }

    public static ECPublicKey decodePublicKey(byte[] uncompressedKey) {
        if (uncompressedKey == null || uncompressedKey.length != 65 || uncompressedKey[0] != 0x04) {
            throw new IllegalArgumentException("Invalid uncompressed EC public key: must be 65 bytes starting with 0x04");
        }
        try {
            ECParameterSpec spec = getEcParameterSpec();
            byte[] xBytes = Arrays.copyOfRange(uncompressedKey, 1, 33);
            byte[] yBytes = Arrays.copyOfRange(uncompressedKey, 33, 65);
            BigInteger x = new BigInteger(1, xBytes);
            BigInteger y = new BigInteger(1, yBytes);
            ECPoint point = new ECPoint(x, y);
            ECPublicKeySpec keySpec = new ECPublicKeySpec(point, spec);
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode EC public key: " + e.getMessage(), e);
        }
    }

    public static ECPrivateKey decodePrivateKey(byte[] privateKeyBytes) {
        if (privateKeyBytes == null || (privateKeyBytes.length != 32 && privateKeyBytes.length != 33)) {
            throw new IllegalArgumentException("Invalid EC private key bytes: must be 32 bytes");
        }
        try {
            ECParameterSpec spec = getEcParameterSpec();
            BigInteger s = new BigInteger(1, privateKeyBytes);
            ECPrivateKeySpec keySpec = new ECPrivateKeySpec(s, spec);
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode EC private key: " + e.getMessage(), e);
        }
    }

    public static byte[] encodePublicKey(ECPublicKey key) {
        ECPoint w = key.getW();
        byte[] x = toFixed32Bytes(w.getAffineX());
        byte[] y = toFixed32Bytes(w.getAffineY());
        byte[] result = new byte[65];
        result[0] = 0x04;
        System.arraycopy(x, 0, result, 1, 32);
        System.arraycopy(y, 0, result, 33, 32);
        return result;
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(CURVE_NAME));
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate EC key pair", e);
        }
    }

    public static byte[] toFixed32Bytes(BigInteger n) {
        byte[] raw = n.toByteArray();
        byte[] fixed = new byte[32];
        if (raw.length == 32) {
            return raw;
        } else if (raw.length > 32) {
            System.arraycopy(raw, raw.length - 32, fixed, 0, 32);
        } else {
            System.arraycopy(raw, 0, fixed, 32 - raw.length, raw.length);
        }
        return fixed;
    }

    public static byte[] hkdfExtract(byte[] salt, byte[] ikm) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            byte[] actualSalt = (salt == null || salt.length == 0) ? new byte[32] : salt;
            mac.init(new SecretKeySpec(actualSalt, HMAC_SHA256));
            return mac.doFinal(ikm);
        } catch (Exception e) {
            throw new IllegalStateException("HKDF-Extract failed", e);
        }
    }

    public static byte[] hkdfExpand(byte[] prk, byte[] info, int length) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(prk, HMAC_SHA256));
            ByteBuffer out = ByteBuffer.allocate(length);
            byte[] t = new byte[0];
            int block = 1;
            while (out.hasRemaining()) {
                mac.reset();
                mac.update(t);
                if (info != null && info.length > 0) {
                    mac.update(info);
                }
                mac.update((byte) block);
                t = mac.doFinal();
                int toCopy = Math.min(t.length, out.remaining());
                out.put(t, 0, toCopy);
                block++;
            }
            return out.array();
        } catch (Exception e) {
            throw new IllegalStateException("HKDF-Expand failed", e);
        }
    }

    /**
     * Encrypts plaintext payload in accordance with RFC 8291 (aes128gcm).
     */
    public static byte[] encryptPayload(byte[] plaintext, byte[] subscriberP256dh, byte[] subscriberAuth) {
        try {
            ECPublicKey subscriberKey = decodePublicKey(subscriberP256dh);
            KeyPair asKeyPair = generateKeyPair();
            ECPublicKey asPublicKey = (ECPublicKey) asKeyPair.getPublic();
            byte[] asPublicKeyBytes = encodePublicKey(asPublicKey);

            // ECDH Shared Secret
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(asKeyPair.getPrivate());
            ka.doPhase(subscriberKey, true);
            byte[] ecdhSecret = ka.generateSecret();

            // WebPush Info: "WebPush: info\0" || subscriberPublicKey (65) || asPublicKey (65)
            ByteBuffer infoBuf = ByteBuffer.allocate(WEBPUSH_INFO_PREFIX.length + 65 + 65);
            infoBuf.put(WEBPUSH_INFO_PREFIX);
            infoBuf.put(subscriberP256dh);
            infoBuf.put(asPublicKeyBytes);
            byte[] authInfo = infoBuf.array();

            byte[] prkKey = hkdfExtract(subscriberAuth, ecdhSecret);
            byte[] ikm = hkdfExpand(prkKey, authInfo, 32);

            // 16-byte random salt
            byte[] salt = new byte[16];
            SECURE_RANDOM.nextBytes(salt);

            byte[] cekPrk = hkdfExtract(salt, ikm);
            byte[] cek = hkdfExpand(cekPrk, AES128GCM_INFO, 16);
            byte[] nonce = hkdfExpand(cekPrk, NONCE_INFO, 12);

            // Record: plaintext || 0x02 delimiter
            byte[] record = new byte[plaintext.length + 1];
            System.arraycopy(plaintext, 0, record, 0, plaintext.length);
            record[plaintext.length] = 0x02;

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(record);

            // Binary body: salt (16) || rs (4) || idlen (1) || asPublicKey (65) || ciphertext
            int rs = 4096;
            ByteBuffer body = ByteBuffer.allocate(16 + 4 + 1 + 65 + ciphertext.length);
            body.put(salt);
            body.putInt(rs);
            body.put((byte) 65);
            body.put(asPublicKeyBytes);
            body.put(ciphertext);

            return body.array();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt Web Push payload: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a signed VAPID JWT (RFC 8292) using ES256 with IEEE P1363 64-byte signature.
     */
    public static String createVapidJwt(String audienceOrigin, String subject, PrivateKey privateKey) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (12 * 3600); // 12 hours

            String headerJson = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
            String claimsJson = String.format("{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}",
                    audienceOrigin, exp, subject);

            String encodedHeader = encodeBase64Url(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedClaims = encodeBase64Url(claimsJson.getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedClaims;

            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] sigBytes = signature.sign();

            return signingInput + "." + encodeBase64Url(sigBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create VAPID JWT: " + e.getMessage(), e);
        }
    }

    public static String extractOrigin(String endpoint) {
        try {
            URI uri = URI.create(endpoint.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            return scheme + "://" + host + (port != -1 ? ":" + port : "");
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract origin from endpoint: " + endpoint, e);
        }
    }
}
