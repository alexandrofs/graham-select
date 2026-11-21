package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataWhatsappNotificationLogRepository extends JpaRepository<WhatsappNotificationLogEntity, UUID> {
    boolean existsByDedupKey(String dedupKey);
    Optional<WhatsappNotificationLogEntity> findByDedupKey(String dedupKey);
    List<WhatsappNotificationLogEntity> findByUserIdOrderBySentAtDesc(String userId, Pageable pageable);
}
