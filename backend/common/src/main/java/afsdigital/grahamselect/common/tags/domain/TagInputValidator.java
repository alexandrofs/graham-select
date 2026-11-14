package afsdigital.grahamselect.common.tags.domain;

import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure domain validator and normalizer for Tag names, colors, and tickers.
 */
@Slf4j
public final class TagInputValidator {

    public static final String DEFAULT_COLOR = "#6366F1";
    public static final int MAX_TAG_NAME_LENGTH = 50;
    public static final int MAX_TICKER_LENGTH = 20;
    public static final int MAX_TAGS_PER_ASSET = 100;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9 ._%+-]{0,19}$");

    private TagInputValidator() {
    }

    public static String validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw invalidArgument("userId é obrigatório.");
        }
        return userId.strip();
    }

    public static String validateAndNormalizeName(String rawName) {
        if (rawName == null) {
            throw invalidArgument("Nome da tag é obrigatório.");
        }
        if (rawName.chars().anyMatch(Character::isISOControl)) {
            throw invalidArgument("Nome da tag não pode conter quebras de linha ou caracteres de controle.");
        }
        String trimmed = rawName.strip();
        if (trimmed.isEmpty()) {
            throw invalidArgument("Nome da tag é obrigatório.");
        }
        if (trimmed.length() > MAX_TAG_NAME_LENGTH) {
            throw invalidArgument("Nome da tag deve ter no máximo 50 caracteres.");
        }
        return trimmed;
    }

    public static String toNormalizedNameKey(String validName) {
        if (validName == null || validName.isBlank()) {
            throw invalidArgument("Nome da tag é obrigatório.");
        }
        return validName.strip().toLowerCase(Locale.ROOT);
    }

    public static String validateAndNormalizeColor(String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            return DEFAULT_COLOR;
        }
        String trimmed = rawColor.strip();
        if (!HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
            throw invalidArgument("Cor da tag deve seguir o formato hexadecimal #RRGGBB.");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static String validateAndNormalizeTicker(String rawTicker) {
        if (rawTicker == null) {
            throw invalidArgument("Ticker é obrigatório.");
        }
        if (rawTicker.chars().anyMatch(Character::isISOControl)) {
            throw invalidArgument("Ticker contém caracteres de controle inválidos.");
        }
        String normalized = rawTicker.strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw invalidArgument("Ticker é obrigatório.");
        }
        if (normalized.contains("/") || normalized.contains("\\") || normalized.contains("..")) {
            throw invalidArgument("Ticker inválido.");
        }
        if (!TICKER_PATTERN.matcher(normalized).matches()) {
            throw invalidArgument(
                    "Ticker inválido. Deve conter de 1 a 20 caracteres alfanuméricos válidos.");
        }
        return normalized;
    }

    private static IllegalArgumentException invalidArgument(String message) {
        IllegalArgumentException ex = new IllegalArgumentException(message);
        log.error("Tag domain validation failed: {}", message, ex);
        return ex;
    }
}
