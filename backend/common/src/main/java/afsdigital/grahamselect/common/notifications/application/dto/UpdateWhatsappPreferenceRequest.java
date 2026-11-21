package afsdigital.grahamselect.common.notifications.application.dto;

import java.math.BigDecimal;

public record UpdateWhatsappPreferenceRequest(
        String phoneNumber,
        boolean optedIn,
        Boolean notifyDividends,
        Boolean notifyRebalanceTriggers,
        Boolean notifyGoalMilestones,
        BigDecimal rebalanceDriftThresholdPct
) {
}
