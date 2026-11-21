package afsdigital.grahamselect.common.notifications.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WhatsappPreferenceDto(
        String phoneNumber,
        boolean optedIn,
        OffsetDateTime optedInAt,
        boolean notifyDividends,
        boolean notifyRebalanceTriggers,
        boolean notifyGoalMilestones,
        BigDecimal rebalanceDriftThresholdPct
) {
}
