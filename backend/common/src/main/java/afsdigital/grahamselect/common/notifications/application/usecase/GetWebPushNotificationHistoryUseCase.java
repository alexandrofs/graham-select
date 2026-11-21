package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WebPushNotificationLogDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushNotificationLogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetWebPushNotificationHistoryUseCase {

    private final WebPushNotificationLogPort logPort;

    public List<WebPushNotificationLogDto> execute(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        int resolvedLimit = Math.max(1, Math.min(limit, 100));
        log.info("Buscando histórico de Web Push para userId={}, limit={}", userId, resolvedLimit);

        return logPort.findRecentByUserId(userId, resolvedLimit).stream()
                .map(entry -> new WebPushNotificationLogDto(
                        entry.getId(),
                        entry.getTicker(),
                        entry.getEventType() != null ? entry.getEventType().name() : null,
                        entry.getTitle(),
                        entry.getMessagePreview(),
                        entry.getProvider(),
                        entry.getHttpStatus(),
                        entry.getStatus() != null ? entry.getStatus().name() : null,
                        entry.getErrorMessage(),
                        entry.getSentAt()
                ))
                .toList();
    }
}
