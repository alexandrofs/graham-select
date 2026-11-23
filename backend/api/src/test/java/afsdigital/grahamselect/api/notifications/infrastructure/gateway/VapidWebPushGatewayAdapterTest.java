package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
                "mailto:kerno@afsdigital.com.br",
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @SuppressWarnings("unchecked")
    void shouldFallbackTitleToKernoAndDefaultUrlWhenTitleIsNullOrBlankAndUrlIsNull(String inputTitle) throws Exception {
        ObjectMapper spyObjectMapper = spy(new ObjectMapper());
        VapidWebPushGatewayAdapter spyAdapter = new VapidWebPushGatewayAdapter(
                publicKeyBase64,
                privateKeyBase64,
                "mailto:kerno@afsdigital.com.br",
                5000,
                10000,
                spyObjectMapper
        );
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer localMockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(spyAdapter, "restClient", restClientBuilder.build());

        String endpoint = "https://fcm.googleapis.com/fcm/send/fake-token";
        WebPushSubscription subscription = createSubscription(endpoint);
        AtomicReference<String> capturedAuthHeader = new AtomicReference<>();

        localMockServer.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> capturedAuthHeader.set(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .andRespond(withStatus(HttpStatus.CREATED));

        WebPushSendResult result = spyAdapter.sendNotification(
                subscription, inputTitle, "Nova oportunidade identificada!", null
        );

        assertThat(result.success()).isTrue();
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(spyObjectMapper).writeValueAsString(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("title", "Kerno")
                .containsEntry("body", "Nova oportunidade identificada!")
                .containsEntry("url", "/graham-recommendations");

        String authHeader = capturedAuthHeader.get();
        assertThat(authHeader).startsWith("vapid t=");
        String jwt = authHeader.substring("vapid t=".length(), authHeader.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);
        String claimsJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode claims = spyObjectMapper.readTree(claimsJson);
        assertThat(claims.get("sub").asText()).isEqualTo("mailto:kerno@afsdigital.com.br");

        localMockServer.verify();
    }

    @Test
    void shouldBindKernoVapidSubjectFromApplicationYmlWhenProviderIsVapid() {
        new ApplicationContextRunner()
                .withUserConfiguration(VapidWebPushGatewayAdapter.class)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withInitializer(ctx -> {
                    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                    try {
                        loader.load("application", new ClassPathResource("application.yml"))
                                .forEach(ps -> ctx.getEnvironment().getPropertySources().addLast(ps));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .withPropertyValues(
                        "webpush.provider=vapid",
                        "webpush.vapid.public-key=" + publicKeyBase64,
                        "webpush.vapid.private-key=" + privateKeyBase64
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VapidWebPushGatewayAdapter.class);
                    VapidWebPushGatewayAdapter bean = context.getBean(VapidWebPushGatewayAdapter.class);
                    assertThat(ReflectionTestUtils.getField(bean, "subject"))
                            .isEqualTo("mailto:kerno@afsdigital.com.br");
                });
    }

    @Test
    void shouldReturnFailedResultWhenPushServiceReturnsServerError() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/fake-token";
        WebPushSubscription subscription = createSubscription(endpoint);

        mockServer.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Push gateway unavailable"));

        WebPushSendResult result = adapter.sendNotification(
                subscription, null, "Corpo do alerta", null
        );

        assertThat(result.success()).isFalse();
        assertThat(result.expired()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(500);
        assertThat(result.errorMessage()).isEqualTo("Push gateway unavailable");
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
