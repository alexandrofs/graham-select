package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSendResult;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;

public interface WebPushGatewayPort {
    WebPushSendResult sendNotification(WebPushSubscription subscription, String title, String body, String url);
    String getVapidPublicKey();
    String getProviderName();
}
