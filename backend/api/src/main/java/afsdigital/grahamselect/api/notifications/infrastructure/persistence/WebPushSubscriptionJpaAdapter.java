package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WebPushMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebPushSubscriptionJpaAdapter implements WebPushSubscriptionPort {

    private final SpringDataWebPushSubscriptionRepository repository;

    @Override
    public Optional<WebPushSubscription> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<WebPushSubscription> findByEndpointHash(String endpointHash) {
        return repository.findByEndpointHash(endpointHash).map(this::toDomain);
    }

    @Override
    public List<WebPushSubscription> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WebPushSubscription> findActiveByUserId(String userId) {
        return repository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public WebPushSubscription save(WebPushSubscription subscription) {
        WebPushSubscriptionEntity entity = WebPushSubscriptionEntity.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .endpoint(subscription.getEndpoint())
                .endpointHash(subscription.getEndpointHash())
                .p256dhKey(subscription.getP256dhKey())
                .authKey(subscription.getAuthKey())
                .userAgent(WebPushMaskingUtils.truncate(subscription.getUserAgent(), 500))
                .active(subscription.isActive())
                .lastUsedAt(subscription.getLastUsedAt() != null
                        ? subscription.getLastUsedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                        : null)
                .createdAt(subscription.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .updatedAt(subscription.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .build();
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private WebPushSubscription toDomain(WebPushSubscriptionEntity entity) {
        return WebPushSubscription.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .endpoint(entity.getEndpoint())
                .endpointHash(entity.getEndpointHash())
                .p256dhKey(entity.getP256dhKey())
                .authKey(entity.getAuthKey())
                .userAgent(entity.getUserAgent())
                .active(entity.isActive())
                .lastUsedAt(entity.getLastUsedAt() != null ? entity.getLastUsedAt().atOffset(ZoneOffset.UTC) : null)
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .build();
    }
}
