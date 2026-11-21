package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WhatsappPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class GetWhatsappPreferencesUseCase {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("5.00");

    private final WhatsappPreferencePort preferencePort;

    public WhatsappPreferenceDto execute(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        return preferencePort.findByUserId(userId)
                .map(pref -> new WhatsappPreferenceDto(
                        pref.getPhoneNumber(),
                        pref.isOptedIn(),
                        pref.getOptedInAt(),
                        pref.isNotifyDividends(),
                        pref.isNotifyRebalanceTriggers(),
                        pref.isNotifyGoalMilestones(),
                        pref.getRebalanceDriftThresholdPct() != null
                                ? pref.getRebalanceDriftThresholdPct()
                                : DEFAULT_THRESHOLD
                ))
                .orElseGet(() -> new WhatsappPreferenceDto(
                        "",
                        false,
                        null,
                        true,
                        true,
                        true,
                        DEFAULT_THRESHOLD
                ));
    }
}
