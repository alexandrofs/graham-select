package afsdigital.grahamselect.api.goals.infrastructure.persistence;

import afsdigital.grahamselect.api.goals.infrastructure.persistence.jpa.entities.FinancialGoalEntity;
import afsdigital.grahamselect.api.goals.infrastructure.persistence.jpa.repository.FinancialGoalJpaRepository;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FinancialGoalRepositoryImpl implements FinancialGoalRepository {

    private final FinancialGoalJpaRepository jpaRepository;

    @Override
    public Optional<FinancialGoal> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    public FinancialGoal save(FinancialGoal goal) {
        FinancialGoalEntity entity = toEntity(goal);
        FinancialGoalEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FinancialGoal> findByIdAndUserId(UUID id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId)
                .map(this::toDomain);
    }

    private FinancialGoal toDomain(FinancialGoalEntity entity) {
        return FinancialGoal.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .goalType(entity.getGoalType())
                .targetValue(entity.getTargetValue())
                .monthlyContribution(entity.getMonthlyContribution())
                .estimatedYears(entity.getEstimatedYears())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private FinancialGoalEntity toEntity(FinancialGoal goal) {
        return FinancialGoalEntity.builder()
                .id(goal.getId())
                .userId(goal.getUserId())
                .goalType(goal.getGoalType())
                .targetValue(goal.getTargetValue())
                .monthlyContribution(goal.getMonthlyContribution())
                .estimatedYears(goal.getEstimatedYears())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
