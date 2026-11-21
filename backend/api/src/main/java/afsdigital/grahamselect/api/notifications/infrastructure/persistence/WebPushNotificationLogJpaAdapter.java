package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WebPushMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushDeliveryStatus;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushNotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WebPushNotificationLogJpaAdapter implements WebPushNotificationLogPort {

    private final SpringDataWebPushNotificationLogRepository repository;

    @Override
    public Optional<WebPushNotificationLog> findByDedupKey(String dedupKey) {
        String boundedKey = WebPushMaskingUtils.boundDedupKey(dedupKey, 255);
        return repository.findByDedupKey(boundedKey).map(this::toDomain);
    }

    @Override
    public WebPushNotificationLog save(WebPushNotificationLog entry) {
        WebPushNotificationLogEntity entity = WebPushNotificationLogEntity.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .subscriptionId(entry.getSubscriptionId())
                .ticker(entry.getTicker() != null ? WebPushMaskingUtils.truncate(entry.getTicker(), 20) : null)
                .eventType(entry.getEventType() != null ? entry.getEventType().name() : "UNKNOWN")
                .dedupKey(WebPushMaskingUtils.boundDedupKey(entry.getDedupKey(), 255))
                .title(WebPushMaskingUtils.truncate(entry.getTitle(), 150))
                .messagePreview(WebPushMaskingUtils.truncate(entry.getMessagePreview(), 500))
                .provider(WebPushMaskingUtils.truncate(entry.getProvider() != null ? entry.getProvider() : "UNKNOWN", 50))
                .httpStatus(entry.getHttpStatus())
                .status(entry.getStatus() != null ? entry.getStatus().name() : WebPushDeliveryStatus.FAILED.name())
                .errorMessage(entry.getErrorMessage())
                .sentAt(entry.getSentAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .build();
        return toDomain(repository.save(entity));
    }

    @Override
    public List<WebPushNotificationLog> findRecentByUserId(String userId, int limit) {
        return repository.findByUserIdOrderBySentAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private WebPushNotificationLog toDomain(WebPushNotificationLogEntity entity) {
        WebPushAlertType eventType = null;
        if (entity.getEventType() != null) {
            try {
                eventType = WebPushAlertType.valueOf(entity.getEventType());
            } catch (IllegalArgumentException ignored) {
            }
        }

        WebPushDeliveryStatus status = WebPushDeliveryStatus.FAILED;
        if (entity.getStatus() != null) {
            try {
                status = WebPushDeliveryStatus.valueOf(entity.getStatus());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return WebPushNotificationLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .subscriptionId(entity.getSubscriptionId())
                .ticker(entity.getTicker())
                .eventType(eventType)
                .dedupKey(entity.getDedupKey())
                .title(entity.getTitle())
                .messagePreview(entity.getMessagePreview())
                .provider(entity.getProvider())
                .httpStatus(entity.getHttpStatus())
                .status(status)
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt().atOffset(ZoneOffset.UTC))
                .build();
    }
}
