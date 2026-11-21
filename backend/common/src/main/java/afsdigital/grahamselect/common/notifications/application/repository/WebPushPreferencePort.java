package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;

import java.util.List;
import java.util.Optional;

public interface WebPushPreferencePort {
    Optional<WebPushPreference> findByUserId(String userId);
    List<WebPushPreference> findAllOptedIn();
    WebPushPreference save(WebPushPreference preference);
}
