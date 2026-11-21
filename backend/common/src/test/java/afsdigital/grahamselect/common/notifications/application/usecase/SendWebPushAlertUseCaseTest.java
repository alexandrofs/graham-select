package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushDeliveryStatus;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushNotificationLog;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendWebPushAlertUseCaseTest {

    @Mock
    private WebPushNotificationLogPort logPort;

    @Mock
    private WebPushSubscriptionPort subscriptionPort;

    @Mock
    private WebPushGatewayPort gatewayPort;

    @InjectMocks
    private SendWebPushAlertUseCase useCase;

    private WebPushSubscription createSubscription(String userId, boolean active) {
        return WebPushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .endpoint("https://fcm.googleapis.com/fcm/send/12345")
                .endpointHash("fake_hash")
                .p256dhKey("fake_p256dh")
                .authKey("fake_auth")
                .userAgent("TestAgent")
                .active(active)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    @Test
    void shouldSendAlertSuccessfullyAndRecordSentLog() {
        String userId = "user-123";
        WebPushSubscription sub = createSubscription(userId, true);
        String dedupKey = "test:123";

        when(logPort.findByDedupKey(dedupKey)).thenReturn(Optional.empty());
        when(gatewayPort.sendNotification(eq(sub), any(), any(), any()))
                .thenReturn(new WebPushSendResult(true, false, 201, "MOCK", null));

        boolean sent = useCase.sendAlert(
                userId, sub, "PETR4", WebPushAlertType.GRAHAM_BUY_ZONE,
                dedupKey, "Buy Zone PETR4", "Margin 30%", "/graham-recommendations"
        );

        assertThat(sent).isTrue();
        ArgumentCaptor<WebPushNotificationLog> logCaptor = ArgumentCaptor.forClass(WebPushNotificationLog.class);
        verify(logPort).save(logCaptor.capture());
        WebPushNotificationLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(WebPushDeliveryStatus.SENT);
        assertThat(savedLog.getHttpStatus()).isEqualTo(201);
        assertThat(savedLog.getProvider()).isEqualTo("MOCK");
        assertThat(savedLog.getTicker()).isEqualTo("PETR4");

        verify(subscriptionPort).save(sub);
        assertThat(sub.getLastUsedAt()).isNotNull();
    }

    @Test
    void shouldSkipSendingWhenDedupKeyAlreadyHasSentStatus() {
        String userId = "user-123";
        WebPushSubscription sub = createSubscription(userId, true);
        String dedupKey = "test:123";

        WebPushNotificationLog existing = WebPushNotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .dedupKey(dedupKey)
                .status(WebPushDeliveryStatus.SENT)
                .build();

        when(logPort.findByDedupKey(dedupKey)).thenReturn(Optional.of(existing));

        boolean sent = useCase.sendAlert(
                userId, sub, "PETR4", WebPushAlertType.GRAHAM_BUY_ZONE,
                dedupKey, "Title", "Body", "/url"
        );

        assertThat(sent).isFalse();
        verify(gatewayPort, never()).sendNotification(any(), any(), any(), any());
    }

    @Test
    void shouldAllowRetryWhenPreviousAttemptFailedReusingLogId() {
        String userId = "user-123";
        WebPushSubscription sub = createSubscription(userId, true);
        String dedupKey = "test:123";
        UUID previousLogId = UUID.randomUUID();

        WebPushNotificationLog existing = WebPushNotificationLog.builder()
                .id(previousLogId)
                .userId(userId)
                .dedupKey(dedupKey)
                .status(WebPushDeliveryStatus.FAILED)
                .build();

        when(logPort.findByDedupKey(dedupKey)).thenReturn(Optional.of(existing));
        when(gatewayPort.sendNotification(eq(sub), any(), any(), any()))
                .thenReturn(new WebPushSendResult(true, false, 201, "MOCK", null));

        boolean sent = useCase.sendAlert(
                userId, sub, "VALE3", WebPushAlertType.GRAHAM_BUY_ZONE,
                dedupKey, "Title", "Body", "/url"
        );

        assertThat(sent).isTrue();
        ArgumentCaptor<WebPushNotificationLog> logCaptor = ArgumentCaptor.forClass(WebPushNotificationLog.class);
        verify(logPort).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getId()).isEqualTo(previousLogId);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(WebPushDeliveryStatus.SENT);
    }

    @Test
    void shouldDeactivateSubscriptionWhenEndpointExpired() {
        String userId = "user-123";
        WebPushSubscription sub = createSubscription(userId, true);
        String dedupKey = "test:expired";

        when(logPort.findByDedupKey(dedupKey)).thenReturn(Optional.empty());
        when(gatewayPort.sendNotification(eq(sub), any(), any(), any()))
                .thenReturn(new WebPushSendResult(false, true, 410, "VAPID", "Subscription gone"));

        boolean sent = useCase.sendAlert(
                userId, sub, "BBAS3", WebPushAlertType.GRAHAM_BUY_ZONE,
                dedupKey, "Title", "Body", "/url"
        );

        assertThat(sent).isFalse();
        assertThat(sub.isActive()).isFalse();
        verify(subscriptionPort).save(sub);

        ArgumentCaptor<WebPushNotificationLog> logCaptor = ArgumentCaptor.forClass(WebPushNotificationLog.class);
        verify(logPort).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(WebPushDeliveryStatus.EXPIRED);
        assertThat(logCaptor.getValue().getHttpStatus()).isEqualTo(410);
    }

    @Test
    void shouldSkipSendingWhenDedupKeyAlreadyHasExpiredStatus() {
        String userId = "user-123";
        WebPushSubscription sub = createSubscription(userId, true);
        String dedupKey = "test:expired-dedup";

        WebPushNotificationLog existing = WebPushNotificationLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .dedupKey(dedupKey)
                .status(WebPushDeliveryStatus.EXPIRED)
                .build();

        when(logPort.findByDedupKey(dedupKey)).thenReturn(Optional.of(existing));

        boolean sent = useCase.sendAlert(
                userId, sub, "PETR4", WebPushAlertType.GRAHAM_BUY_ZONE,
                dedupKey, "Title", "Body", "/url"
        );

        assertThat(sent).isFalse();
        verify(gatewayPort, never()).sendNotification(any(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenUserIdIsBlank() {
        WebPushSubscription sub = createSubscription("user-123", true);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                useCase.sendAlert(
                        "   ", sub, "PETR4", WebPushAlertType.GRAHAM_BUY_ZONE,
                        "dedup:1", "Title", "Body", "/url"
                )
        );
    }
}
