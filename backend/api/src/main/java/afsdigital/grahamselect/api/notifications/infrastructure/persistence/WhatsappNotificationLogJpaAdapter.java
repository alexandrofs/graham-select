package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WhatsappMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappDeliveryStatus;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WhatsappNotificationLogJpaAdapter implements WhatsappNotificationLogPort {

    private final SpringDataWhatsappNotificationLogRepository repository;

    @Override
    public boolean existsByDedupKey(String dedupKey) {
        return repository.existsByDedupKey(dedupKey);
    }

    @Override
    public Optional<WhatsappNotificationLog> findByDedupKey(String dedupKey) {
        return repository.findByDedupKey(dedupKey).map(this::toDomain);
    }

    @Override
    public WhatsappNotificationLog save(WhatsappNotificationLog logEntry) {
        WhatsappNotificationLogEntity entity = WhatsappNotificationLogEntity.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .eventType(logEntry.getEventType().name())
                .dedupKey(WhatsappMaskingUtils.boundDedupKey(logEntry.getDedupKey(), 255))
                .messagePreview(WhatsappMaskingUtils.truncate(logEntry.getMessagePreview(), 500))
                .provider(WhatsappMaskingUtils.truncate(logEntry.getProvider(), 50))
                .providerMessageId(logEntry.getProviderMessageId() != null
                        ? WhatsappMaskingUtils.truncate(logEntry.getProviderMessageId(), 128)
                        : null)
                .status(logEntry.getStatus().name())
                .errorMessage(logEntry.getErrorMessage())
                .sentAt(logEntry.getSentAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .build();
        return toDomain(repository.save(entity));
    }

    @Override
    public List<WhatsappNotificationLog> findRecentByUserId(String userId, int limit) {
        return repository.findByUserIdOrderBySentAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private WhatsappNotificationLog toDomain(WhatsappNotificationLogEntity entity) {
        return WhatsappNotificationLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .eventType(WhatsappAlertType.valueOf(entity.getEventType()))
                .dedupKey(entity.getDedupKey())
                .messagePreview(entity.getMessagePreview())
                .provider(entity.getProvider())
                .providerMessageId(entity.getProviderMessageId())
                .status(WhatsappDeliveryStatus.valueOf(entity.getStatus()))
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt().atOffset(ZoneOffset.UTC))
                .build();
    }
}
