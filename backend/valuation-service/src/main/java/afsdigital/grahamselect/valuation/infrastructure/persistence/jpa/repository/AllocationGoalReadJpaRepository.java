package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.AllocationGoalReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationGoalReadJpaRepository extends JpaRepository<AllocationGoalReadEntity, String> {
    List<AllocationGoalReadEntity> findByUserId(String userId);
}
