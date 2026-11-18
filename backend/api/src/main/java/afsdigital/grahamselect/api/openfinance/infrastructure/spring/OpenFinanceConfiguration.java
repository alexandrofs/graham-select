package afsdigital.grahamselect.api.openfinance.infrastructure.spring;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceSyncLogPort;
import afsdigital.grahamselect.common.openfinance.application.repository.TokenEncryptionPort;
import afsdigital.grahamselect.common.openfinance.application.usecase.CompleteOpenFinanceConsentUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.InitiateOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.ListOpenFinanceInstitutionsUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.ListUserOpenFinanceConnectionsUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.RevokeOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.SyncOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SendPortfolioUpdateNotificationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFinanceConfiguration {

    @Bean
    public ListOpenFinanceInstitutionsUseCase listOpenFinanceInstitutionsUseCase() {
        return new ListOpenFinanceInstitutionsUseCase();
    }

    @Bean
    public InitiateOpenFinanceConnectionUseCase initiateOpenFinanceConnectionUseCase(
            OpenFinanceConnectionPort connectionPort,
            OpenFinanceConnectorPort connectorPort,
            FamilyProfilePort familyProfilePort
    ) {
        return new InitiateOpenFinanceConnectionUseCase(connectionPort, connectorPort, familyProfilePort);
    }

    @Bean
    public CompleteOpenFinanceConsentUseCase completeOpenFinanceConsentUseCase(
            OpenFinanceConnectionPort connectionPort,
            OpenFinanceConnectorPort connectorPort,
            TokenEncryptionPort tokenEncryptionPort,
            SyncOpenFinanceConnectionUseCase syncConnectionUseCase
    ) {
        return new CompleteOpenFinanceConsentUseCase(connectionPort, connectorPort, tokenEncryptionPort, syncConnectionUseCase);
    }

    @Bean
    public SyncOpenFinanceConnectionUseCase syncOpenFinanceConnectionUseCase(
            OpenFinanceConnectionPort connectionPort,
            OpenFinanceSyncLogPort syncLogPort,
            OpenFinanceConnectorPort connectorPort,
            TokenEncryptionPort tokenEncryptionPort,
            SaveTradeUseCase saveTradeUseCase,
            SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase
    ) {
        return new SyncOpenFinanceConnectionUseCase(
                connectionPort,
                syncLogPort,
                connectorPort,
                tokenEncryptionPort,
                saveTradeUseCase,
                sendPortfolioUpdateNotificationUseCase
        );
    }

    @Bean
    public RevokeOpenFinanceConnectionUseCase revokeOpenFinanceConnectionUseCase(
            OpenFinanceConnectionPort connectionPort,
            OpenFinanceConnectorPort connectorPort,
            TokenEncryptionPort tokenEncryptionPort
    ) {
        return new RevokeOpenFinanceConnectionUseCase(connectionPort, connectorPort, tokenEncryptionPort);
    }

    @Bean
    public ListUserOpenFinanceConnectionsUseCase listUserOpenFinanceConnectionsUseCase(
            OpenFinanceConnectionPort connectionPort
    ) {
        return new ListUserOpenFinanceConnectionsUseCase(connectionPort);
    }
}
