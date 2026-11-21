package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WebPushPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
public class GetWebPushPreferencesUseCase {

    private static final BigDecimal DEFAULT_MIN_MARGIN = new BigDecimal("20.00");

    private final WebPushPreferencePort preferencePort;

    public WebPushPreferenceDto execute(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        log.info("Buscando preferências de Web Push para userId={}", userId);

        return preferencePort.findByUserId(userId)
                .map(this::toDto)
                .orElseGet(() -> new WebPushPreferenceDto(
                        false,
                        null,
                        DEFAULT_MIN_MARGIN,
                        true
                ));
    }

    private WebPushPreferenceDto toDto(WebPushPreference preference) {
        return new WebPushPreferenceDto(
                preference.isOptedIn(),
                preference.getOptedInAt(),
                preference.getMinMarginOfSafetyPct() != null ? preference.getMinMarginOfSafetyPct() : DEFAULT_MIN_MARGIN,
                preference.isOnlyCustodyTickers()
        );
    }
}
