package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WhatsappNotificationLogDto;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetWhatsappNotificationHistoryUseCase {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final WhatsappNotificationLogPort logPort;

    public List<WhatsappNotificationLogDto> execute(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return logPort.findRecentByUserId(userId, safeLimit).stream()
                .map(entry -> new WhatsappNotificationLogDto(
                        entry.getId(),
                        entry.getEventType().name(),
                        entry.getMessagePreview(),
                        entry.getProvider(),
                        entry.getStatus().name(),
                        entry.getErrorMessage(),
                        entry.getSentAt()
                ))
                .toList();
    }
}
