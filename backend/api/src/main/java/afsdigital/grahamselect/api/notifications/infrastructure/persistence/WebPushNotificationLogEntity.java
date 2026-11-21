package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "web_push_notification_logs", indexes = {
        @Index(name = "idx_web_push_logs_user_sent", columnList = "user_id, sent_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPushNotificationLogEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "subscription_id", length = 36)
    private UUID subscriptionId;

    @Column(name = "ticker", length = 20)
    private String ticker;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "dedup_key", nullable = false, unique = true, length = 255)
    private String dedupKey;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message_preview", nullable = false, length = 500)
    private String messagePreview;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
