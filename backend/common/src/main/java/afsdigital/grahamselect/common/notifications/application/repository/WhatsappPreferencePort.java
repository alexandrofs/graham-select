package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;

import java.util.List;
import java.util.Optional;

public interface WhatsappPreferencePort {
    Optional<WhatsappNotificationPreference> findByUserId(String userId);
    List<WhatsappNotificationPreference> findAllOptedIn();
    WhatsappNotificationPreference save(WhatsappNotificationPreference preference);
}
