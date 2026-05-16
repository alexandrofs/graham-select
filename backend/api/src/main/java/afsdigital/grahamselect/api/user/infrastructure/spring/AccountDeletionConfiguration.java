package afsdigital.grahamselect.api.user.infrastructure.spring;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import afsdigital.grahamselect.common.user.application.usecase.CancelAccountDeletionUseCase;
import afsdigital.grahamselect.common.user.application.usecase.ExecuteAccountPurgeUseCase;
import afsdigital.grahamselect.common.user.application.usecase.RequestAccountDeletionUseCase;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountDeletionConfiguration {

    @Bean
    public RequestAccountDeletionUseCase requestAccountDeletionUseCase(AccountDeletionRequestRepository repository) {
        return new RequestAccountDeletionUseCase(repository);
    }

    @Bean
    public CancelAccountDeletionUseCase cancelAccountDeletionUseCase(AccountDeletionRequestRepository repository) {
        return new CancelAccountDeletionUseCase(repository);
    }

    @Bean
    public ExecuteAccountPurgeUseCase executeAccountPurgeUseCase(
            AccountDeletionRequestRepository deletionRequestRepository,
            UserDataPurgePort userDataPurgePort,
            UserRepository userRepository) {
        return new ExecuteAccountPurgeUseCase(deletionRequestRepository, userDataPurgePort, userRepository);
    }
}
