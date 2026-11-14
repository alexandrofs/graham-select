package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;

import java.util.List;
import java.util.UUID;

public interface AllocationGoalPort {
    void saveAllocationGoals(String userId, List<AllocationGoalDto> goals);
    List<AllocationGoalDto> findByUserId(String userId);

    default void saveAllocationGoals(String userId, UUID tagId, List<AllocationGoalDto> goals) {
        saveAllocationGoals(userId, goals);
    }

    default List<AllocationGoalDto> findByUserId(String userId, UUID tagId) {
        return findByUserId(userId);
    }
}
