package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.RegisterWebPushSubscriptionRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushSubscriptionDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RegisterWebPushSubscriptionUseCase {

    private final WebPushSubscriptionPort subscriptionPort;

    public WebPushSubscriptionDto execute(String userId, RegisterWebPushSubscriptionRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (request.endpoint() == null || request.endpoint().isBlank()) {
            throw new IllegalArgumentException("endpoint cannot be null or blank");
        }
        if (request.p256dhKey() == null || request.p256dhKey().isBlank()) {
            throw new IllegalArgumentException("p256dhKey cannot be null or blank");
        }
        if (request.authKey() == null || request.authKey().isBlank()) {
            throw new IllegalArgumentException("authKey cannot be null or blank");
        }

        validateEndpoint(request.endpoint());

        String endpointHash = WebPushMaskingUtils.endpointHash(userId, request.endpoint());
        String maskedEndpoint = WebPushMaskingUtils.maskEndpoint(request.endpoint());
        String truncatedUserAgent = WebPushMaskingUtils.truncate(request.userAgent(), 500);

        log.info("Registrando assinatura Web Push para userId={}, endpoint={}", userId, maskedEndpoint);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        deactivateOtherUsersForEndpoint(userId, request.endpoint().trim(), now);
        var existingOpt = subscriptionPort.findByEndpointHash(endpointHash);

        WebPushSubscription subscription;
        if (existingOpt.isPresent()) {
            subscription = existingOpt.get();
            subscription.setP256dhKey(request.p256dhKey().trim());
            subscription.setAuthKey(request.authKey().trim());
            subscription.setUserAgent(truncatedUserAgent);
            subscription.setActive(true);
            subscription.setUpdatedAt(now);
        } else {
            subscription = WebPushSubscription.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .endpoint(request.endpoint().trim())
                    .endpointHash(endpointHash)
                    .p256dhKey(request.p256dhKey().trim())
                    .authKey(request.authKey().trim())
                    .userAgent(truncatedUserAgent)
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        WebPushSubscription saved = subscriptionPort.save(subscription);
        return new WebPushSubscriptionDto(
                saved.getId(),
                maskedEndpoint,
                saved.getUserAgent(),
                saved.isActive(),
                saved.getLastUsedAt(),
                saved.getCreatedAt()
        );
    }

    private void deactivateOtherUsersForEndpoint(String activeUserId, String endpoint, OffsetDateTime now) {
        var existingByEndpoint = subscriptionPort.findByEndpoint(endpoint);
        if (existingByEndpoint == null || existingByEndpoint.isEmpty()) {
            return;
        }
        for (WebPushSubscription other : existingByEndpoint) {
            if (other != null && other.isActive() && !activeUserId.equals(other.getUserId())) {
                log.info("Desativando assinatura Web Push de outro usuário no mesmo endpoint físico: previousUserId={}, newUserId={}",
                        other.getUserId(), activeUserId);
                other.setActive(false);
                other.setUpdatedAt(now);
                subscriptionPort.save(other);
            }
        }
    }

    private void validateEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null) {
                throw new IllegalArgumentException("Endpoint URL must specify a scheme");
            }
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Endpoint URL must specify a valid host");
            }
            if ("https".equalsIgnoreCase(scheme)) {
                return;
            }
            if ("http".equalsIgnoreCase(scheme) && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))) {
                return;
            }
            throw new IllegalArgumentException("Endpoint must use HTTPS or localhost/127.0.0.1 for local development");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid push endpoint URL: " + e.getMessage(), e);
        }
    }
}
