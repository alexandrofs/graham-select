package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.usecase.GetAllocationGoalsUseCase;
import afsdigital.grahamselect.valuation.application.usecase.SaveAllocationGoalsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AllocationStrategyConfiguration {

    @Bean
    public SaveAllocationGoalsUseCase saveAllocationGoalsUseCase(AllocationGoalPort allocationGoalPort) {
        return new SaveAllocationGoalsUseCase(allocationGoalPort);
    }

    @Bean
    public GetAllocationGoalsUseCase getAllocationGoalsUseCase(AllocationGoalPort allocationGoalPort) {
        return new GetAllocationGoalsUseCase(allocationGoalPort);
    }
}
