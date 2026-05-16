package afsdigital.grahamselect.api.user.infrastructure.spring;

import afsdigital.grahamselect.common.user.application.usecase.UpdateInvestorProfileUseCase;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserProfileConfiguration {

    @Bean
    public UpdateInvestorProfileUseCase updateInvestorProfileUseCase(UserRepository userRepository) {
        return new UpdateInvestorProfileUseCase(userRepository);
    }
}
