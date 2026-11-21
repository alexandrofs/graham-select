package afsdigital.grahamselect.common.notifications.application.usecase;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class WebPushMaskingUtils {

    private WebPushMaskingUtils() {
    }

    public static String maskEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "****";
        }
        try {
            URI uri = URI.create(endpoint.trim());
            String host = uri.getHost();
            String scheme = uri.getScheme();
            int port = uri.getPort();
            String origin = scheme + "://" + host + (port != -1 ? ":" + port : "");
            return origin + "/.../****";
        } catch (Exception e) {
            String trimmed = endpoint.trim();
            if (trimmed.length() <= 12) {
                return "****";
            }
            return trimmed.substring(0, 8) + ".../****";
        }
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

    public static String boundDedupKey(String dedupKey, int maxLength) {
        if (dedupKey == null || dedupKey.length() <= maxLength) {
            return dedupKey;
        }
        String hash = sha256Hex(dedupKey).substring(0, 16);
        int prefixLength = Math.max(0, maxLength - 17);
        return dedupKey.substring(0, prefixLength) + ":" + hash;
    }

    public static String endpointHash(String userId, String endpoint) {
        String normalizedEndpoint = endpoint != null ? endpoint.trim() : "";
        return sha256Hex((userId != null ? userId : "") + "|" + normalizedEndpoint);
    }

    public static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
