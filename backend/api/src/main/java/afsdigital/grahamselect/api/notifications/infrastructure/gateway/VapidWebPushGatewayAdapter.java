package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WebPushMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "webpush.provider", havingValue = "vapid")
public class VapidWebPushGatewayAdapter implements WebPushGatewayPort {

    private static final String PROVIDER = "VAPID";
    private static final long JWT_CACHE_EXPIRY_SECONDS = 12L * 3600L;
    private static final long JWT_REFRESH_BUFFER_SECONDS = 300L;

    private record CachedJwt(String token, long expiresAtEpochSeconds) {
    }

    private final String publicKey;
    private final String privateKeyRaw;
    private final String subject;
    private final ECPrivateKey privateKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CachedJwt> jwtCache = new ConcurrentHashMap<>();

    public VapidWebPushGatewayAdapter(
            @Value("${webpush.vapid.public-key:}") String publicKey,
            @Value("${webpush.vapid.private-key:}") String privateKeyRaw,
            @Value("${webpush.vapid.subject:}") String subject,
            @Value("${webpush.http.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${webpush.http.read-timeout-ms:10000}") int readTimeoutMs,
            ObjectMapper objectMapper
    ) {
        if (publicKey == null || publicKey.isBlank()) {
            throw new IllegalArgumentException("VAPID public key must not be blank when webpush.provider=vapid");
        }
        if (privateKeyRaw == null || privateKeyRaw.isBlank()) {
            throw new IllegalArgumentException("VAPID private key must not be blank when webpush.provider=vapid");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("VAPID subject must not be blank when webpush.provider=vapid");
        }

        this.publicKey = publicKey.trim();
        this.privateKeyRaw = privateKeyRaw.trim();
        this.subject = subject.trim();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        byte[] privBytes = WebPushCryptoUtils.decodeBase64Url(this.privateKeyRaw);
        this.privateKey = WebPushCryptoUtils.decodePrivateKey(privBytes);
        byte[] pubBytes = WebPushCryptoUtils.decodeBase64Url(this.publicKey);
        ECPublicKey ecPublicKey = WebPushCryptoUtils.decodePublicKey(pubBytes);
        validateKeyPair(this.privateKey, ecPublicKey);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        log.info("VapidWebPushGatewayAdapter inicializado com sucesso para subject={}", this.subject);
    }

    @Override
    public WebPushSendResult sendNotification(WebPushSubscription subscription, String title, String body, String url) {
        if (subscription == null || subscription.getEndpoint() == null || subscription.getEndpoint().isBlank()) {
            return new WebPushSendResult(false, false, 400, PROVIDER, "Assinatura ou endpoint inválido");
        }

        String endpoint = subscription.getEndpoint();
        String maskedEndpoint = WebPushMaskingUtils.maskEndpoint(endpoint);

        try {
            String origin = WebPushCryptoUtils.extractOrigin(endpoint);
            String vapidJwt = getOrCreateVapidJwt(origin);

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("title", (title != null && !title.isBlank()) ? title : "Kerno");
            payloadMap.put("body", body != null ? body : "");
            payloadMap.put("url", (url != null && !url.isBlank()) ? url : "/graham-recommendations");
            byte[] payloadJson = objectMapper.writeValueAsString(payloadMap).getBytes(StandardCharsets.UTF_8);

            byte[] subscriberP256dh = WebPushCryptoUtils.decodeBase64Url(subscription.getP256dhKey());
            byte[] subscriberAuth = WebPushCryptoUtils.decodeBase64Url(subscription.getAuthKey());

            byte[] encryptedBody = WebPushCryptoUtils.encryptPayload(payloadJson, subscriberP256dh, subscriberAuth);

            log.info("Enviando Web Push via VAPID: endpoint={}, title={}", maskedEndpoint, title);

            var responseEntity = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Encoding", "aes128gcm")
                    .header("TTL", "86400")
                    .header("Urgency", "normal")
                    .header(HttpHeaders.AUTHORIZATION, "vapid t=" + vapidJwt + ", k=" + this.publicKey)
                    .body(encryptedBody)
                    .retrieve()
                    .toBodilessEntity();

            HttpStatusCode statusCode = responseEntity.getStatusCode();
            int httpStatus = statusCode.value();

            if (statusCode.is2xxSuccessful()) {
                return new WebPushSendResult(true, false, httpStatus, PROVIDER, null);
            }

            return new WebPushSendResult(false, false, httpStatus, PROVIDER, "Status HTTP inesperado: " + httpStatus);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            boolean expired = (status == 404 || status == 410);

            if (expired) {
                log.info("Assinatura Web Push expirada/removida no push service: status={}, endpoint={}", status, maskedEndpoint);
            } else {
                log.error("Erro HTTP ao enviar Web Push: status={}, endpoint={}, body={}: {}",
                        status, maskedEndpoint, responseBody, e.getMessage(), e);
            }
            return new WebPushSendResult(false, expired, status, PROVIDER, responseBody);
        } catch (Exception e) {
            log.error("Falha ao cifrar ou despachar Web Push para endpoint={}: {}", maskedEndpoint, e.getMessage(), e);
            return new WebPushSendResult(false, false, 500, PROVIDER, e.getMessage());
        }
    }

    private String getOrCreateVapidJwt(String origin) {
        long now = Instant.now().getEpochSecond();
        CachedJwt cached = jwtCache.get(origin);
        if (cached != null && (cached.expiresAtEpochSeconds() - now) > JWT_REFRESH_BUFFER_SECONDS) {
            return cached.token();
        }
        String freshToken = WebPushCryptoUtils.createVapidJwt(origin, this.subject, this.privateKey);
        jwtCache.put(origin, new CachedJwt(freshToken, now + JWT_CACHE_EXPIRY_SECONDS));
        return freshToken;
    }

    private static void validateKeyPair(ECPrivateKey privateKey, ECPublicKey publicKey) {
        try {
            byte[] probe = "kerno-vapid-keypair-check".getBytes(StandardCharsets.UTF_8);
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(probe);
            byte[] signature = signer.sign();

            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(probe);
            if (!verifier.verify(signature)) {
                throw new IllegalArgumentException("VAPID public key does not match private key");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate VAPID key pair: " + e.getMessage(), e);
        }
    }

    @Override
    public String getVapidPublicKey() {
        return publicKey;
    }

    @Override
    public String getProviderName() {
        return PROVIDER;
    }
}
