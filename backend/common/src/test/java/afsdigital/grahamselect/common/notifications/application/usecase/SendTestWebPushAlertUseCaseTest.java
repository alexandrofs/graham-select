package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WebPushTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendTestWebPushAlertUseCaseTest {

    @Mock
    private WebPushPreferencePort preferencePort;

    @Mock
    private WebPushSubscriptionPort subscriptionPort;

    @Mock
    private SendWebPushAlertUseCase sendWebPushAlertUseCase;

    @Mock
    private WebPushGatewayPort gatewayPort;

    @InjectMocks
    private SendTestWebPushAlertUseCase useCase;

    @Test
    void shouldSendTestAlertToAllActiveSubscriptions() {
        String userId = "user-123";
        WebPushPreference pref = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(true)
                .build();

        WebPushSubscription sub1 = WebPushSubscription.builder().id(UUID.randomUUID()).userId(userId).endpoint("https://sub1").active(true).build();
        WebPushSubscription sub2 = WebPushSubscription.builder().id(UUID.randomUUID()).userId(userId).endpoint("https://sub2").active(true).build();

        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(subscriptionPort.findActiveByUserId(userId)).thenReturn(List.of(sub1, sub2));
        when(gatewayPort.getProviderName()).thenReturn("MOCK");

        when(sendWebPushAlertUseCase.sendAlert(eq(userId), eq(sub1), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        when(sendWebPushAlertUseCase.sendAlert(eq(userId), eq(sub2), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        WebPushTestAlertResultDto result = useCase.execute(userId);

        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.provider()).isEqualTo("MOCK");
    }

    @Test
    void shouldRejectWhenUserNotOptedIn() {
        String userId = "user-123";
        WebPushPreference pref = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(false)
                .build();

        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(pref));

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentimento");
    }

    @Test
    void shouldRejectWhenNoActiveSubscriptionsExist() {
        String userId = "user-123";
        WebPushPreference pref = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(true)
                .build();

        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(subscriptionPort.findActiveByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositivo ativo");
    }
}
