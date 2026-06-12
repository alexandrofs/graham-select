package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationGoalJpaRepository extends JpaRepository<AllocationGoalEntity, UUID> {
    List<AllocationGoalEntity> findByUserId(String userId);
    void deleteByUserId(String userId);
}
