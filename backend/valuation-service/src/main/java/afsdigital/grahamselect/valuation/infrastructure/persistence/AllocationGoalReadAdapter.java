package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.AllocationGoalReadEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.AllocationGoalReadJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AllocationGoalReadAdapter implements AllocationGoalPort {

    private final AllocationGoalReadJpaRepository repository;

    @Override
    public void saveAllocationGoals(String userId, List<AllocationGoalDto> goals) {
        throw new UnsupportedOperationException("valuation-service is read-only for allocation goals");
    }

    @Override
    public List<AllocationGoalDto> findByUserId(String userId) {
        return findByUserId(userId, null);
    }

    @Override
    public List<AllocationGoalDto> findByUserId(String userId, UUID tagId) {
        String scopeKey = AllocationScope.fromTagId(tagId);
        List<AllocationGoalReadEntity> entities = repository.findByUserIdAndScopeKey(userId, scopeKey);
        return entities.stream()
                .map(e -> new AllocationGoalDto(
                        e.getGoalType(),
                        e.getTargetKey(),
                        e.getTargetPercentage()
                ))
                .toList();
    }
}
