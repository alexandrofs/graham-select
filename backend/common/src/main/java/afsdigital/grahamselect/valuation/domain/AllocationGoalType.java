package afsdigital.grahamselect.valuation.domain;

import java.util.Locale;

public enum AllocationGoalType {
    TICKER,
    ASSET_CLASS;

    public boolean matches(String rawGoalType) {
        return rawGoalType != null && name().equalsIgnoreCase(rawGoalType.trim().toUpperCase(Locale.ROOT));
    }
}
