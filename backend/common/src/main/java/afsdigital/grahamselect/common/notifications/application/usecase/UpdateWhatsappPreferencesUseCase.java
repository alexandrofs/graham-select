package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.UpdateWhatsappPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class UpdateWhatsappPreferencesUseCase {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{9,14}$");
    private static final BigDecimal MIN_THRESHOLD = new BigDecimal("1.00");
    private static final BigDecimal MAX_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("5.00");

    private final WhatsappPreferencePort preferencePort;

    public WhatsappPreferenceDto execute(String userId, UpdateWhatsappPreferenceRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String normalizedPhone = request.phoneNumber() != null ? request.phoneNumber().trim() : "";
        boolean requiresPhoneValidation = request.optedIn() || !normalizedPhone.isEmpty();
        if (requiresPhoneValidation && !E164_PATTERN.matcher(normalizedPhone).matches()) {
            throw new IllegalArgumentException("Número de WhatsApp inválido. Utilize o formato E.164 (ex: +5511999999999).");
        }

        BigDecimal clampedThreshold = clampThreshold(request.rebalanceDriftThresholdPct());
        boolean notifyDividends = request.notifyDividends() != null ? request.notifyDividends() : true;
        boolean notifyRebalance = request.notifyRebalanceTriggers() != null ? request.notifyRebalanceTriggers() : true;
        boolean notifyGoals = request.notifyGoalMilestones() != null ? request.notifyGoalMilestones() : true;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        WhatsappNotificationPreference existing = preferencePort.findByUserId(userId).orElse(null);

        OffsetDateTime optedInAt;
        if (request.optedIn()) {
            if (existing != null && existing.isOptedIn() && existing.getOptedInAt() != null) {
                optedInAt = existing.getOptedInAt();
            } else {
                optedInAt = now;
            }
        } else {
            optedInAt = null;
        }

        WhatsappNotificationPreference toSave = WhatsappNotificationPreference.builder()
                .id(existing != null ? existing.getId() : UUID.randomUUID())
                .userId(userId)
                .phoneNumber(normalizedPhone)
                .optedIn(request.optedIn())
                .optedInAt(optedInAt)
                .notifyDividends(notifyDividends)
                .notifyRebalanceTriggers(notifyRebalance)
                .notifyGoalMilestones(notifyGoals)
                .rebalanceDriftThresholdPct(clampedThreshold)
                .createdAt(existing != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build();

        WhatsappNotificationPreference saved = preferencePort.save(toSave);
        log.info("Preferências WhatsApp atualizadas para userId={}, phone={}, optedIn={}, threshold={}%",
                userId,
                WhatsappMaskingUtils.maskPhoneNumber(saved.getPhoneNumber()),
                saved.isOptedIn(),
                saved.getRebalanceDriftThresholdPct());

        return new WhatsappPreferenceDto(
                saved.getPhoneNumber(),
                saved.isOptedIn(),
                saved.getOptedInAt(),
                saved.isNotifyDividends(),
                saved.isNotifyRebalanceTriggers(),
                saved.isNotifyGoalMilestones(),
                saved.getRebalanceDriftThresholdPct()
        );
    }

    private BigDecimal clampThreshold(BigDecimal raw) {
        if (raw == null) {
            return DEFAULT_THRESHOLD;
        }
        BigDecimal scaled = raw.setScale(2, RoundingMode.HALF_UP);
        if (scaled.compareTo(MIN_THRESHOLD) < 0) {
            return MIN_THRESHOLD;
        }
        if (scaled.compareTo(MAX_THRESHOLD) > 0) {
            return MAX_THRESHOLD;
        }
        return scaled;
    }
}
