package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebPushSubscriptionPort {
    Optional<WebPushSubscription> findById(UUID id);
    Optional<WebPushSubscription> findByEndpointHash(String endpointHash);
    default List<WebPushSubscription> findByEndpoint(String endpoint) {
        return List.of();
    }
    List<WebPushSubscription> findByUserId(String userId);
    List<WebPushSubscription> findActiveByUserId(String userId);
    WebPushSubscription save(WebPushSubscription subscription);
    void delete(UUID id);
}
