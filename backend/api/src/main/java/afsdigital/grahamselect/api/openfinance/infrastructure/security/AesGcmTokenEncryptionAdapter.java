package afsdigital.grahamselect.api.openfinance.infrastructure.security;

import afsdigital.grahamselect.common.openfinance.application.repository.TokenEncryptionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Component
public class AesGcmTokenEncryptionAdapter implements TokenEncryptionPort {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int REQUIRED_KEY_LENGTH_BYTES = 32;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmTokenEncryptionAdapter(@Value("${openfinance.encryption.secret-key:0123456789abcdef0123456789abcdef}") String secretKeyString) {
        if (secretKeyString == null) {
            throw new IllegalArgumentException("Secret key must not be null");
        }
        byte[] keyBytes;
        if (secretKeyString.trim().matches("^[0-9a-fA-F]{64}$")) {
            keyBytes = HexFormat.of().parseHex(secretKeyString.trim());
        } else {
            keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < REQUIRED_KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Secret key must be at least 32 bytes (256 bits). Provided: " + keyBytes.length + " bytes");
        }
        byte[] validKey = new byte[REQUIRED_KEY_LENGTH_BYTES];
        System.arraycopy(keyBytes, 0, validKey, 0, REQUIRED_KEY_LENGTH_BYTES);
        this.secretKey = new SecretKeySpec(validKey, ALGORITHM);
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (plainText.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Erro ao cifrar credencial Open Finance com AES-256-GCM: {}", e.getMessage(), e);
            throw new SecurityException("Falha ao cifrar credencial em repouso", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (cipherText.isEmpty()) {
            return "";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            if (decoded.length < IV_LENGTH_BYTE + 16) {
                log.error("Ciphertext recebido para decifração é menor que o tamanho mínimo de IV + Tag ({} bytes)", decoded.length);
                throw new IllegalArgumentException("Ciphertext inválido ou corrompido");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plainBytes = cipher.doFinal(encryptedBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Erro ao decifrar credencial Open Finance com AES-256-GCM: {}", e.getMessage(), e);
            throw new SecurityException("Falha ao decifrar credencial em repouso", e);
        }
    }
}
