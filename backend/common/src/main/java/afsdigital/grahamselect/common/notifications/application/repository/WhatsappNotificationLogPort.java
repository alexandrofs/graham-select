package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationLog;

import java.util.List;
import java.util.Optional;

public interface WhatsappNotificationLogPort {
    boolean existsByDedupKey(String dedupKey);
    Optional<WhatsappNotificationLog> findByDedupKey(String dedupKey);
    WhatsappNotificationLog save(WhatsappNotificationLog log);
    List<WhatsappNotificationLog> findRecentByUserId(String userId, int limit);
}
