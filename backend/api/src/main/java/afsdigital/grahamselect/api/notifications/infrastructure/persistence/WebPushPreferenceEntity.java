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
@Table(name = "web_push_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WebPushPreferenceEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "opted_in", nullable = false)
    private boolean optedIn;

    @Column(name = "opted_in_at")
    private LocalDateTime optedInAt;

    @Column(name = "min_margin_of_safety_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal minMarginOfSafetyPct;

    @Column(name = "only_custody_tickers", nullable = false)
    private boolean onlyCustodyTickers;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
