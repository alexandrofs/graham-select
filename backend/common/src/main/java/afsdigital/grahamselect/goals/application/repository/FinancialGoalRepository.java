package afsdigital.grahamselect.goals.application.repository;

import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;

import java.util.Optional;
import java.util.UUID;

public interface FinancialGoalRepository {

    Optional<FinancialGoal> findByUserId(String userId);

    FinancialGoal save(FinancialGoal goal);

    Optional<FinancialGoal> findByIdAndUserId(UUID id, String userId);
}
