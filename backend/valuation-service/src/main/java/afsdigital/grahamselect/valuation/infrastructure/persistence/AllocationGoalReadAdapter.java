package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.AllocationGoalReadEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.AllocationGoalReadJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class AllocationGoalReadAdapter implements AllocationGoalPort {

    private final AllocationGoalReadJpaRepository repository;

    @Override
    public void saveAllocationGoals(String userId, List<AllocationGoalDto> goals) {
        throw new UnsupportedOperationException("valuation-service is read-only for allocation goals");
    }

    @Override
    public List<AllocationGoalDto> findByUserId(String userId) {
        List<AllocationGoalReadEntity> entities = repository.findByUserId(userId);
        return entities.stream()
                .map(e -> new AllocationGoalDto(
                        e.getGoalType(),
                        e.getTargetKey(),
                        e.getTargetPercentage()
                ))
                .toList();
    }
}
