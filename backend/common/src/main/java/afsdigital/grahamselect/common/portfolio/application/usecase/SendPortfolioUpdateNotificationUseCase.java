package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SendPortfolioUpdateNotificationUseCase {
    private final NotificationPort notificationPort;
    
    public void execute(String userId) {
        log.info("Sending portfolio update notification for user: {}", userId);
        notificationPort.sendPortfolioUpdate(userId);
    }
}
