package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.NotificationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendPortfolioUpdateNotificationUseCaseTest {

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase;

    @Test
    void shouldSendNotificationSuccessfully() {
        String userId = "user-123";

        sendPortfolioUpdateNotificationUseCase.execute(userId);

        verify(notificationPort).sendPortfolioUpdate(userId);
    }
}
