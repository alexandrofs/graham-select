package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class GetFinancialGoalUseCase {

    private final FinancialGoalRepository repository;

    public Optional<FinancialGoalDto> execute(String userId) {
        log.info("Fetching financial goals for user {}", userId);

        return repository.findByUserId(userId)
                .map(this::toDto);
    }

    private FinancialGoalDto toDto(FinancialGoal goal) {
        return new FinancialGoalDto(
                goal.getId(),
                goal.getGoalType().name(),
                goal.getTargetValue(),
                goal.getMonthlyContribution(),
                goal.getEstimatedYears(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
