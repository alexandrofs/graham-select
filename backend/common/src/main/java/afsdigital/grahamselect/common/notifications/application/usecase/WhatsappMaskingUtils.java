package afsdigital.grahamselect.common.notifications.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class WhatsappMaskingUtils {

    private WhatsappMaskingUtils() {
    }

    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "****";
        }
        String trimmed = phoneNumber.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        int prefixLength = Math.min(4, trimmed.length() - 8);
        String prefix = trimmed.substring(0, prefixLength);
        String suffix = trimmed.substring(trimmed.length() - 4);
        return prefix + "****" + suffix;
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /**
     * Bounds a deduplication key to {@code maxLength} characters, preserving uniqueness
     * via a SHA-256 suffix when truncation is required.
     */
    public static String boundDedupKey(String dedupKey, int maxLength) {
        if (dedupKey == null || dedupKey.length() <= maxLength) {
            return dedupKey;
        }
        String hash = sha256Hex(dedupKey).substring(0, 16);
        int prefixLength = Math.max(0, maxLength - 17);
        return dedupKey.substring(0, prefixLength) + ":" + hash;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
