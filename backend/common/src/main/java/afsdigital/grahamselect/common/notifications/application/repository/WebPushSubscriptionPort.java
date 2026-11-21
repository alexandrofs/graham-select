package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebPushSubscriptionPort {
    Optional<WebPushSubscription> findById(UUID id);
    Optional<WebPushSubscription> findByEndpointHash(String endpointHash);
    List<WebPushSubscription> findByUserId(String userId);
    List<WebPushSubscription> findActiveByUserId(String userId);
    WebPushSubscription save(WebPushSubscription subscription);
    void delete(UUID id);
}
