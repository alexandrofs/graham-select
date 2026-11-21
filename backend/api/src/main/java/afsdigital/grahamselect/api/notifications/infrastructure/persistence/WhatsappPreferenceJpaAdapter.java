package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WhatsappPreferenceJpaAdapter implements WhatsappPreferencePort {

    private final SpringDataWhatsappPreferenceRepository repository;

    @Override
    public Optional<WhatsappNotificationPreference> findByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public List<WhatsappNotificationPreference> findAllOptedIn() {
        return repository.findByOptedInTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public WhatsappNotificationPreference save(WhatsappNotificationPreference preference) {
        WhatsappNotificationPreferenceEntity entity = WhatsappNotificationPreferenceEntity.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .phoneNumber(preference.getPhoneNumber())
                .optedIn(preference.isOptedIn())
                .optedInAt(preference.getOptedInAt() != null
                        ? preference.getOptedInAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                        : null)
                .notifyDividends(preference.isNotifyDividends())
                .notifyRebalanceTriggers(preference.isNotifyRebalanceTriggers())
                .notifyGoalMilestones(preference.isNotifyGoalMilestones())
                .rebalanceDriftThresholdPct(preference.getRebalanceDriftThresholdPct())
                .createdAt(preference.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .updatedAt(preference.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .build();
        return toDomain(repository.save(entity));
    }

    private WhatsappNotificationPreference toDomain(WhatsappNotificationPreferenceEntity entity) {
        return WhatsappNotificationPreference.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .phoneNumber(entity.getPhoneNumber())
                .optedIn(entity.isOptedIn())
                .optedInAt(entity.getOptedInAt() != null ? entity.getOptedInAt().atOffset(ZoneOffset.UTC) : null)
                .notifyDividends(entity.isNotifyDividends())
                .notifyRebalanceTriggers(entity.isNotifyRebalanceTriggers())
                .notifyGoalMilestones(entity.isNotifyGoalMilestones())
                .rebalanceDriftThresholdPct(entity.getRebalanceDriftThresholdPct())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .build();
    }
}
