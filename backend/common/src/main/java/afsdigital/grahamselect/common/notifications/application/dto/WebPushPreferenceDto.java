package afsdigital.grahamselect.common.notifications.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WebPushPreferenceDto(
        boolean optedIn,
        OffsetDateTime optedInAt,
        BigDecimal minMarginOfSafetyPct,
        boolean onlyCustodyTickers
) {
}
