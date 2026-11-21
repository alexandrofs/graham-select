package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.UpdateWebPushPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class UpdateWebPushPreferencesUseCase {

    private static final BigDecimal MIN_MARGIN = BigDecimal.ZERO;
    private static final BigDecimal MAX_MARGIN = new BigDecimal("90.00");
    private static final BigDecimal DEFAULT_MARGIN = new BigDecimal("20.00");

    private final WebPushPreferencePort preferencePort;

    public WebPushPreferenceDto execute(String userId, UpdateWebPushPreferenceRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        log.info("Atualizando preferências de Web Push para userId={}, optedIn={}, minMargin={}",
                userId, request.optedIn(), request.minMarginOfSafetyPct());

        BigDecimal rawMargin = request.minMarginOfSafetyPct() != null ? request.minMarginOfSafetyPct() : DEFAULT_MARGIN;
        BigDecimal clampedMargin = clamp(rawMargin, MIN_MARGIN, MAX_MARGIN).setScale(2, RoundingMode.HALF_UP);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var existingOpt = preferencePort.findByUserId(userId);

        WebPushPreference preference;
        if (existingOpt.isPresent()) {
            WebPushPreference existing = existingOpt.get();
            OffsetDateTime optedInAt = existing.getOptedInAt();
            if (request.optedIn() && !existing.isOptedIn()) {
                optedInAt = now;
            } else if (!request.optedIn()) {
                optedInAt = null;
            }

            preference = existing.toBuilder()
                    .optedIn(request.optedIn())
                    .optedInAt(optedInAt)
                    .minMarginOfSafetyPct(clampedMargin)
                    .onlyCustodyTickers(request.onlyCustodyTickers())
                    .updatedAt(now)
                    .build();
        } else {
            OffsetDateTime optedInAt = request.optedIn() ? now : null;
            preference = WebPushPreference.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .optedIn(request.optedIn())
                    .optedInAt(optedInAt)
                    .minMarginOfSafetyPct(clampedMargin)
                    .onlyCustodyTickers(request.onlyCustodyTickers())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        WebPushPreference saved = preferencePort.save(preference);
        return new WebPushPreferenceDto(
                saved.isOptedIn(),
                saved.getOptedInAt(),
                saved.getMinMarginOfSafetyPct(),
                saved.isOnlyCustodyTickers()
        );
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
