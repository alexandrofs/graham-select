package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataWebPushPreferenceRepository extends JpaRepository<WebPushPreferenceEntity, UUID> {
    Optional<WebPushPreferenceEntity> findByUserId(String userId);
    List<WebPushPreferenceEntity> findByOptedInTrue();
}
