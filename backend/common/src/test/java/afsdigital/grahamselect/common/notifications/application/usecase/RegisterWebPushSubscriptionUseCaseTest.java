package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.RegisterWebPushSubscriptionRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushSubscriptionDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWebPushSubscriptionUseCaseTest {

    @Mock
    private WebPushSubscriptionPort subscriptionPort;

    @InjectMocks
    private RegisterWebPushSubscriptionUseCase useCase;

    @Test
    void shouldRegisterNewValidHttpsSubscription() {
        String userId = "user-123";
        String endpoint = "https://fcm.googleapis.com/fcm/send/fake-token";
        String endpointHash = WebPushMaskingUtils.endpointHash(userId, endpoint);

        when(subscriptionPort.findByEndpointHash(endpointHash)).thenReturn(Optional.empty());
        when(subscriptionPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterWebPushSubscriptionRequest request = new RegisterWebPushSubscriptionRequest(
                endpoint,
                "BL99_p256dh_fake_key",
                "auth_fake_secret_key",
                "Mozilla/5.0 (Windows NT 10.0)"
        );

        WebPushSubscriptionDto result = useCase.execute(userId, request);

        assertThat(result.id()).isNotNull();
        assertThat(result.active()).isTrue();
        assertThat(result.userAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0)");
        assertThat(result.endpointPreview()).contains("https://fcm.googleapis.com");
    }

    @Test
    void shouldAcceptLocalhostAnd127EndpointsForLocalDevelopment() {
        String userId = "user-123";
        when(subscriptionPort.findByEndpointHash(any())).thenReturn(Optional.empty());
        when(subscriptionPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterWebPushSubscriptionRequest req1 = new RegisterWebPushSubscriptionRequest(
                "http://localhost:8080/push/1",
                "key1",
                "auth1",
                "agent1"
        );
        RegisterWebPushSubscriptionRequest req2 = new RegisterWebPushSubscriptionRequest(
                "http://127.0.0.1:8080/push/2",
                "key2",
                "auth2",
                "agent2"
        );

        assertThat(useCase.execute(userId, req1)).isNotNull();
        assertThat(useCase.execute(userId, req2)).isNotNull();
    }

    @Test
    void shouldRejectInsecureHttpNonLocalhostEndpoint() {
        String userId = "user-123";
        RegisterWebPushSubscriptionRequest request = new RegisterWebPushSubscriptionRequest(
                "http://insecure-domain.com/push",
                "key1",
                "auth1",
                "agent1"
        );

        assertThatThrownBy(() -> useCase.execute(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void shouldRejectBlankFields() {
        String userId = "user-123";
        assertThatThrownBy(() -> useCase.execute(userId, new RegisterWebPushSubscriptionRequest("", "key", "auth", "ua")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(userId, new RegisterWebPushSubscriptionRequest("https://valid.com", "", "auth", "ua")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(userId, new RegisterWebPushSubscriptionRequest("https://valid.com", "key", "", "ua")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(null, new RegisterWebPushSubscriptionRequest("https://valid.com", "key", "auth", "ua")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReactivateExistingSubscriptionOnReRegistration() {
        String userId = "user-123";
        String endpoint = "https://fcm.googleapis.com/fcm/send/fake-token";
        String endpointHash = WebPushMaskingUtils.endpointHash(userId, endpoint);

        WebPushSubscription existing = WebPushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .endpoint(endpoint)
                .endpointHash(endpointHash)
                .p256dhKey("old_key")
                .authKey("old_auth")
                .active(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        when(subscriptionPort.findByEndpointHash(endpointHash)).thenReturn(Optional.of(existing));
        when(subscriptionPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterWebPushSubscriptionRequest request = new RegisterWebPushSubscriptionRequest(
                endpoint,
                "new_key",
                "new_auth",
                "Updated Agent"
        );

        WebPushSubscriptionDto result = useCase.execute(userId, request);

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.active()).isTrue();
        assertThat(existing.getP256dhKey()).isEqualTo("new_key");
        assertThat(existing.getAuthKey()).isEqualTo("new_auth");
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void shouldRejectHttpsEndpointWithEmptyHost() {
        RegisterWebPushSubscriptionRequest request = new RegisterWebPushSubscriptionRequest(
                "https:///path",
                "key",
                "auth",
                "agent"
        );
        assertThatThrownBy(() -> useCase.execute("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint URL must specify a valid host");
    }
}
