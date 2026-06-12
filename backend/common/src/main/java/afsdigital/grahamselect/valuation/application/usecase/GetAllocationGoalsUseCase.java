package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetAllocationGoalsUseCase {

    private final AllocationGoalPort allocationGoalPort;

    public List<AllocationGoalDto> execute(String userId) {
        log.info("Fetching allocation goals for user {}", userId);
        return allocationGoalPort.findByUserId(userId);
    }
}
