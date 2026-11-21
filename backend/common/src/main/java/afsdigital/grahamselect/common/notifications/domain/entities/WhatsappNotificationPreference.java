package afsdigital.grahamselect.common.notifications.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappNotificationPreference {
    private UUID id;
    private String userId;
    private String phoneNumber;
    private boolean optedIn;
    private OffsetDateTime optedInAt;
    private boolean notifyDividends;
    private boolean notifyRebalanceTriggers;
    private boolean notifyGoalMilestones;
    private BigDecimal rebalanceDriftThresholdPct;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
