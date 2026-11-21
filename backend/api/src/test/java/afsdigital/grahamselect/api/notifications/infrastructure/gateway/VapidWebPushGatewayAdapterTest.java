package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class VapidWebPushGatewayAdapterTest {

    private VapidWebPushGatewayAdapter adapter;
    private MockRestServiceServer mockServer;
    private String publicKeyBase64;
    private String privateKeyBase64;

    @BeforeEach
    void setUp() {
        KeyPair keyPair = WebPushCryptoUtils.generateKeyPair();
        publicKeyBase64 = WebPushCryptoUtils.encodeBase64Url(
                WebPushCryptoUtils.encodePublicKey((ECPublicKey) keyPair.getPublic())
        );
        privateKeyBase64 = WebPushCryptoUtils.encodeBase64Url(
                WebPushCryptoUtils.toFixed32Bytes(((ECPrivateKey) keyPair.getPrivate()).getS())
        );

        adapter = new VapidWebPushGatewayAdapter(
                publicKeyBase64,
                privateKeyBase64,
                "mailto:contato@grahamselect.com.br",
                5000,
                10000,
                new ObjectMapper()
        );

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(adapter, "restClient", restClientBuilder.build());
    }

    private WebPushSubscription createSubscription(String endpoint) {
        KeyPair subKeyPair = WebPushCryptoUtils.generateKeyPair();
        String p256dh = WebPushCryptoUtils.encodeBase64Url(
                WebPushCryptoUtils.encodePublicKey((ECPublicKey) subKeyPair.getPublic())
        );
        String auth = WebPushCryptoUtils.encodeBase64Url("1234567890abcdef".getBytes());

        return WebPushSubscription.builder()
                .id(UUID.randomUUID())
                .userId("user-123")
                .endpoint(endpoint)
                .endpointHash("fake-hash")
                .p256dhKey(p256dh)
                .authKey(auth)
                .active(true)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    @Test
    void shouldSendNotificationSuccessfullyWhenPushServiceReturns201() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/fake-token";
        WebPushSubscription subscription = createSubscription(endpoint);

        mockServer.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Encoding", "aes128gcm"))
                .andExpect(header("TTL", "86400"))
                .andRespond(withStatus(HttpStatus.CREATED));

        WebPushSendResult result = adapter.sendNotification(
                subscription, "Test Title", "Test Body", "/graham-recommendations"
        );

        assertThat(result.success()).isTrue();
        assertThat(result.expired()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(201);
        assertThat(result.provider()).isEqualTo("VAPID");
        mockServer.verify();
    }

    @Test
    void shouldMarkSubscriptionAsExpiredWhenPushServiceReturns410() {
        String endpoint = "https://updates.push.services.mozilla.com/wpush/v2/fake-token";
        WebPushSubscription subscription = createSubscription(endpoint);

        mockServer.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.GONE).body("Subscription has expired or unsubscribed"));

        WebPushSendResult result = adapter.sendNotification(
                subscription, "Test Title", "Test Body", "/graham-recommendations"
        );

        assertThat(result.success()).isFalse();
        assertThat(result.expired()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(410);
        mockServer.verify();
    }

    @Test
    void shouldRejectInitializationWithMissingConfiguration() {
        assertThatThrownBy(() -> new VapidWebPushGatewayAdapter("", privateKeyBase64, "sub", 1000, 1000, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VapidWebPushGatewayAdapter(publicKeyBase64, "", "sub", 1000, 1000, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VapidWebPushGatewayAdapter(publicKeyBase64, privateKeyBase64, "", 1000, 1000, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
