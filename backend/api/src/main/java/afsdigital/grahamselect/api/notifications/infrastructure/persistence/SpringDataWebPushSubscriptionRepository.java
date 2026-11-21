package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataWebPushSubscriptionRepository extends JpaRepository<WebPushSubscriptionEntity, UUID> {
    Optional<WebPushSubscriptionEntity> findByEndpointHash(String endpointHash);
    List<WebPushSubscriptionEntity> findByUserId(String userId);
    List<WebPushSubscriptionEntity> findByUserIdAndActiveTrue(String userId);
}
