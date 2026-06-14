package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.goals.application.dto.CreateFinancialGoalRequest;
import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.application.service.exceptions.FinancialGoalNotFoundException;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class UpdateFinancialGoalUseCase {

    private final FinancialGoalRepository repository;

    public FinancialGoalDto execute(UUID goalId, String userId, CreateFinancialGoalRequest request) {
        log.info("Updating financial goal {} for user {}", goalId, userId);

        FinancialGoal existing = repository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> {
                    log.error("Financial goal not found or access denied for goalId: {}, userId: {}", goalId, userId);
                    return new FinancialGoalNotFoundException(
                            "Financial goal not found or does not belong to user: " + goalId);
                });

        FinancialGoal updated = FinancialGoal.builder()
                .id(existing.getId())
                .userId(existing.getUserId())
                .goalType(request.goalType())
                .targetValue(request.targetValue())
                .monthlyContribution(request.monthlyContribution())
                .estimatedYears(request.estimatedYears())
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        FinancialGoal saved = repository.save(updated);

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
