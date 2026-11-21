package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WebPushSubscriptionDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListWebPushSubscriptionsUseCase {

    private final WebPushSubscriptionPort subscriptionPort;

    public List<WebPushSubscriptionDto> execute(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        log.info("Listando assinaturas Web Push para userId={}", userId);

        return subscriptionPort.findByUserId(userId).stream()
                .map(s -> new WebPushSubscriptionDto(
                        s.getId(),
                        WebPushMaskingUtils.maskEndpoint(s.getEndpoint()),
                        s.getUserAgent(),
                        s.isActive(),
                        s.getLastUsedAt(),
                        s.getCreatedAt()
                ))
                .toList();
    }
}
