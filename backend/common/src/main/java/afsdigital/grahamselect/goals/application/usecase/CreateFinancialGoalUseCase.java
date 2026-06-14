package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.goals.application.dto.CreateFinancialGoalRequest;
import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CreateFinancialGoalUseCase {

    private final FinancialGoalRepository repository;

    public FinancialGoalDto execute(String userId, CreateFinancialGoalRequest request) {
        log.info("Creating financial goal for user {}, type={}, targetValue={}",
                userId, request.goalType(), request.targetValue());

        if (repository.findByUserId(userId).isPresent()) {
            log.error("Failed to create financial goal: user {} already has an active goal", userId);
            throw new IllegalArgumentException("User already has a financial goal");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        FinancialGoal goal = FinancialGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(request.goalType())
                .targetValue(request.targetValue())
                .monthlyContribution(request.monthlyContribution())
                .estimatedYears(request.estimatedYears())
                .createdAt(now)
                .updatedAt(now)
                .build();

        FinancialGoal saved = repository.save(goal);

        log.info("Financial goal created for user {}, type={}, targetValue={}",
                userId, saved.getGoalType(), saved.getTargetValue());

        return toDto(saved);
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
