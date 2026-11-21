package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataWhatsappPreferenceRepository extends JpaRepository<WhatsappNotificationPreferenceEntity, UUID> {
    Optional<WhatsappNotificationPreferenceEntity> findByUserId(String userId);
    List<WhatsappNotificationPreferenceEntity> findByOptedInTrue();
}
