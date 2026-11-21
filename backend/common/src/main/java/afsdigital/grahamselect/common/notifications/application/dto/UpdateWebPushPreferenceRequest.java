package afsdigital.grahamselect.common.notifications.application.dto;

import java.math.BigDecimal;

public record UpdateWebPushPreferenceRequest(
        boolean optedIn,
        BigDecimal minMarginOfSafetyPct,
        boolean onlyCustodyTickers
) {
}
