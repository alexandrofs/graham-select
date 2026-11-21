package afsdigital.grahamselect.api.notifications.infrastructure.persistence;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WebPushPreferenceJpaAdapter implements WebPushPreferencePort {

    private final SpringDataWebPushPreferenceRepository repository;

    @Override
    public Optional<WebPushPreference> findByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public List<WebPushPreference> findAllOptedIn() {
        return repository.findByOptedInTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public WebPushPreference save(WebPushPreference preference) {
        WebPushPreferenceEntity entity = WebPushPreferenceEntity.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .optedIn(preference.isOptedIn())
                .optedInAt(preference.getOptedInAt() != null
                        ? preference.getOptedInAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                        : null)
                .minMarginOfSafetyPct(preference.getMinMarginOfSafetyPct())
                .onlyCustodyTickers(preference.isOnlyCustodyTickers())
                .createdAt(preference.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .updatedAt(preference.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .build();
        return toDomain(repository.save(entity));
    }

    private WebPushPreference toDomain(WebPushPreferenceEntity entity) {
        return WebPushPreference.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .optedIn(entity.isOptedIn())
                .optedInAt(entity.getOptedInAt() != null ? entity.getOptedInAt().atOffset(ZoneOffset.UTC) : null)
                .minMarginOfSafetyPct(entity.getMinMarginOfSafetyPct())
                .onlyCustodyTickers(entity.isOnlyCustodyTickers())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .build();
    }
}
