package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WebPushMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "webpush.provider", havingValue = "mock", matchIfMissing = true)
public class MockWebPushGatewayAdapter implements WebPushGatewayPort {

    private static final String PROVIDER = "MOCK";
    private static final String DEFAULT_MOCK_PUBLIC_KEY =
            "BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U";

    private final String publicKey;

    public MockWebPushGatewayAdapter(
            @Value("${webpush.vapid.public-key:}") String publicKey
    ) {
        this.publicKey = (publicKey != null && !publicKey.isBlank()) ? publicKey : DEFAULT_MOCK_PUBLIC_KEY;
    }

    @Override
    public WebPushSendResult sendNotification(WebPushSubscription subscription, String title, String body, String url) {
        String maskedEndpoint = WebPushMaskingUtils.maskEndpoint(subscription != null ? subscription.getEndpoint() : null);
        log.info("Envio Web Push simulado: provider={}, endpoint={}, title={}, preview={}, url={}",
                PROVIDER,
                maskedEndpoint,
                title,
                WebPushMaskingUtils.truncate(body, 80),
                url);
        return new WebPushSendResult(true, false, 201, PROVIDER, null);
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
