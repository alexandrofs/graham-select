package afsdigital.grahamselect.api.admin.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
public final class AdminAnonymizationUtils {

    private AdminAnonymizationUtils() {
    }

    public static String anonymizeUserId(String rawUserId) {
        String safeInput = (rawUserId == null || rawUserId.isBlank()) ? "anonymous" : rawUserId.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(safeInput.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hashBytes);
            return "usr_***" + hex.substring(0, 4).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available for userId anonymization", e);
            return "usr_***0000";
        }
    }
}
