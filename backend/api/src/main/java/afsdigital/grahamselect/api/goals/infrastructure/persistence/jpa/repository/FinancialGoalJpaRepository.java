package afsdigital.grahamselect.api.goals.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.goals.infrastructure.persistence.jpa.entities.FinancialGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FinancialGoalJpaRepository extends JpaRepository<FinancialGoalEntity, UUID> {

    Optional<FinancialGoalEntity> findByUserId(String userId);

    Optional<FinancialGoalEntity> findByIdAndUserId(UUID id, String userId);
}
