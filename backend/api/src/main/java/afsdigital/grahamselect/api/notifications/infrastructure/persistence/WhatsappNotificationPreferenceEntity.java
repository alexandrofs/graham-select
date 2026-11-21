package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "phoneNumber")
public class WhatsappNotificationPreferenceEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "opted_in", nullable = false)
    private boolean optedIn;

    @Column(name = "opted_in_at")
    private LocalDateTime optedInAt;

    @Column(name = "notify_dividends", nullable = false)
    private boolean notifyDividends;

    @Column(name = "notify_rebalance_triggers", nullable = false)
    private boolean notifyRebalanceTriggers;

    @Column(name = "notify_goal_milestones", nullable = false)
    private boolean notifyGoalMilestones;

    @Column(name = "rebalance_drift_threshold_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal rebalanceDriftThresholdPct;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
