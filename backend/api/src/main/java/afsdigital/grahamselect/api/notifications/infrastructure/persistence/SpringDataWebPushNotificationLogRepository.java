package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataWebPushNotificationLogRepository extends JpaRepository<WebPushNotificationLogEntity, UUID> {
    Optional<WebPushNotificationLogEntity> findByDedupKey(String dedupKey);
    List<WebPushNotificationLogEntity> findByUserIdOrderBySentAtDesc(String userId, Pageable pageable);
}
