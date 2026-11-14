package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.usecase.GetAllocationGoalsUseCase;
import afsdigital.grahamselect.valuation.application.usecase.SaveAllocationGoalsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AllocationStrategyConfiguration {

    @Bean
    public SaveAllocationGoalsUseCase saveAllocationGoalsUseCase(
            AllocationGoalPort allocationGoalPort,
            PortfolioTagPort portfolioTagPort
    ) {
        return new SaveAllocationGoalsUseCase(allocationGoalPort, portfolioTagPort);
    }

    @Bean
    public GetAllocationGoalsUseCase getAllocationGoalsUseCase(
            AllocationGoalPort allocationGoalPort,
            PortfolioTagPort portfolioTagPort
    ) {
        return new GetAllocationGoalsUseCase(allocationGoalPort, portfolioTagPort);
    }
}
