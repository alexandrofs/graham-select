package afsdigital.grahamselect.api.goals.infrastructure.spring;

import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.application.usecase.CreateFinancialGoalUseCase;
import afsdigital.grahamselect.goals.application.usecase.GetFinancialGoalUseCase;
import afsdigital.grahamselect.goals.application.usecase.UpdateFinancialGoalUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoalsServiceConfiguration {

    @Bean
    public CreateFinancialGoalUseCase createFinancialGoalUseCase(FinancialGoalRepository financialGoalRepository) {
        return new CreateFinancialGoalUseCase(financialGoalRepository);
    }

    @Bean
    public UpdateFinancialGoalUseCase updateFinancialGoalUseCase(FinancialGoalRepository financialGoalRepository) {
        return new UpdateFinancialGoalUseCase(financialGoalRepository);
    }

    @Bean
    public GetFinancialGoalUseCase getFinancialGoalUseCase(FinancialGoalRepository financialGoalRepository) {
        return new GetFinancialGoalUseCase(financialGoalRepository);
    }
}
