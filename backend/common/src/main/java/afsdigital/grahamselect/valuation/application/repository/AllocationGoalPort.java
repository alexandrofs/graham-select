package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;

import java.util.List;

public interface AllocationGoalPort {
    void saveAllocationGoals(String userId, List<AllocationGoalDto> goals);
    List<AllocationGoalDto> findByUserId(String userId);
}
