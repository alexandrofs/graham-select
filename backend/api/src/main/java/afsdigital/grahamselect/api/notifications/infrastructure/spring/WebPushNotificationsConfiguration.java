package afsdigital.grahamselect.api.notifications.infrastructure.spring;

import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.application.usecase.DeleteWebPushSubscriptionUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.EvaluateGrahamBuyZoneAlertsUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWebPushNotificationHistoryUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWebPushPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.ListWebPushSubscriptionsUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.RegisterWebPushSubscriptionUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendTestWebPushAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendWebPushAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.UpdateWebPushPreferencesUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebPushNotificationsConfiguration {

    @Bean
    public GetWebPushPreferencesUseCase getWebPushPreferencesUseCase(WebPushPreferencePort preferencePort) {
        return new GetWebPushPreferencesUseCase(preferencePort);
    }

    @Bean
    public UpdateWebPushPreferencesUseCase updateWebPushPreferencesUseCase(WebPushPreferencePort preferencePort) {
        return new UpdateWebPushPreferencesUseCase(preferencePort);
    }

    @Bean
    public RegisterWebPushSubscriptionUseCase registerWebPushSubscriptionUseCase(WebPushSubscriptionPort subscriptionPort) {
        return new RegisterWebPushSubscriptionUseCase(subscriptionPort);
    }

    @Bean
    public ListWebPushSubscriptionsUseCase listWebPushSubscriptionsUseCase(WebPushSubscriptionPort subscriptionPort) {
        return new ListWebPushSubscriptionsUseCase(subscriptionPort);
    }

    @Bean
    public DeleteWebPushSubscriptionUseCase deleteWebPushSubscriptionUseCase(WebPushSubscriptionPort subscriptionPort) {
        return new DeleteWebPushSubscriptionUseCase(subscriptionPort);
    }

    @Bean
    public GetWebPushNotificationHistoryUseCase getWebPushNotificationHistoryUseCase(WebPushNotificationLogPort logPort) {
        return new GetWebPushNotificationHistoryUseCase(logPort);
    }

    @Bean
    public SendWebPushAlertUseCase sendWebPushAlertUseCase(
            WebPushNotificationLogPort logPort,
            WebPushSubscriptionPort subscriptionPort,
            WebPushGatewayPort gatewayPort
    ) {
        return new SendWebPushAlertUseCase(logPort, subscriptionPort, gatewayPort);
    }

    @Bean
    public SendTestWebPushAlertUseCase sendTestWebPushAlertUseCase(
            WebPushPreferencePort preferencePort,
            WebPushSubscriptionPort subscriptionPort,
            SendWebPushAlertUseCase sendWebPushAlertUseCase,
            WebPushGatewayPort gatewayPort
    ) {
        return new SendTestWebPushAlertUseCase(preferencePort, subscriptionPort, sendWebPushAlertUseCase, gatewayPort);
    }

    @Bean
    public EvaluateGrahamBuyZoneAlertsUseCase evaluateGrahamBuyZoneAlertsUseCase(
            WebPushPreferencePort preferencePort,
            WebPushSubscriptionPort subscriptionPort,
            RankingRepository rankingRepository,
            GetCustodyPositionsUseCase getCustodyPositionsUseCase,
            SendWebPushAlertUseCase sendWebPushAlertUseCase
    ) {
        return new EvaluateGrahamBuyZoneAlertsUseCase(
                preferencePort,
                subscriptionPort,
                rankingRepository,
                getCustodyPositionsUseCase,
                sendWebPushAlertUseCase
        );
    }
}
