package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushNotificationLog;

import java.util.List;
import java.util.Optional;

public interface WebPushNotificationLogPort {
    Optional<WebPushNotificationLog> findByDedupKey(String dedupKey);
    WebPushNotificationLog save(WebPushNotificationLog log);
    List<WebPushNotificationLog> findRecentByUserId(String userId, int limit);
}
