package afsdigital.grahamselect.api.notifications.infrastructure.spring;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.application.usecase.EvaluateRebalanceAndGoalAlertsUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWhatsappNotificationHistoryUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWhatsappPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.NotifyDividendAlertsUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendTestWhatsappAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendWhatsappAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.UpdateWhatsappPreferencesUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WhatsappNotificationsConfiguration {

    @Bean
    public GetWhatsappPreferencesUseCase getWhatsappPreferencesUseCase(WhatsappPreferencePort preferencePort) {
        return new GetWhatsappPreferencesUseCase(preferencePort);
    }

    @Bean
    public UpdateWhatsappPreferencesUseCase updateWhatsappPreferencesUseCase(WhatsappPreferencePort preferencePort) {
        return new UpdateWhatsappPreferencesUseCase(preferencePort);
    }

    @Bean
    public GetWhatsappNotificationHistoryUseCase getWhatsappNotificationHistoryUseCase(
            WhatsappNotificationLogPort logPort
    ) {
        return new GetWhatsappNotificationHistoryUseCase(logPort);
    }

    @Bean
    public SendWhatsappAlertUseCase sendWhatsappAlertUseCase(
            WhatsappNotificationLogPort logPort,
            WhatsappGatewayPort gatewayPort
    ) {
        return new SendWhatsappAlertUseCase(logPort, gatewayPort);
    }

    @Bean
    public SendTestWhatsappAlertUseCase sendTestWhatsappAlertUseCase(
            WhatsappPreferencePort preferencePort,
            WhatsappNotificationLogPort logPort,
            WhatsappGatewayPort gatewayPort
    ) {
        return new SendTestWhatsappAlertUseCase(preferencePort, logPort, gatewayPort);
    }

    @Bean
    public NotifyDividendAlertsUseCase notifyDividendAlertsUseCase(
            WhatsappPreferencePort preferencePort,
            GetCustodyPositionsUseCase getCustodyPositionsUseCase,
            SendWhatsappAlertUseCase sendWhatsappAlertUseCase
    ) {
        return new NotifyDividendAlertsUseCase(
                preferencePort,
                getCustodyPositionsUseCase,
                sendWhatsappAlertUseCase
        );
    }

    @Bean
    public EvaluateRebalanceAndGoalAlertsUseCase evaluateRebalanceAndGoalAlertsUseCase(
            WhatsappPreferencePort preferencePort,
            GetCustodyPositionsUseCase getCustodyPositionsUseCase,
            GetPortfolioSummaryUseCase getPortfolioSummaryUseCase,
            AllocationGoalPort allocationGoalPort,
            FinancialGoalRepository financialGoalRepository,
            SendWhatsappAlertUseCase sendWhatsappAlertUseCase
    ) {
        return new EvaluateRebalanceAndGoalAlertsUseCase(
                preferencePort,
                getCustodyPositionsUseCase,
                getPortfolioSummaryUseCase,
                allocationGoalPort,
                financialGoalRepository,
                sendWhatsappAlertUseCase
        );
    }
}
