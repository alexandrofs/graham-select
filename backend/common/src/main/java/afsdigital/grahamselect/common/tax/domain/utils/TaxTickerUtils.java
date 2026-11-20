package afsdigital.grahamselect.common.tax.domain.utils;

import java.util.Locale;

public final class TaxTickerUtils {

    private TaxTickerUtils() {
    }

    public static String stripFractionalSuffix(String ticker) {
        if (ticker == null) {
            return "";
        }
        String normalized = ticker.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith("F") && normalized.length() > 4
                && Character.isDigit(normalized.charAt(normalized.length() - 2))) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
