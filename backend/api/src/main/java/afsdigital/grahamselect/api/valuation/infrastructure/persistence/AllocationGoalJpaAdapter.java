package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repositories.AllocationGoalJpaRepository;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AllocationGoalJpaAdapter implements AllocationGoalPort {

    private final AllocationGoalJpaRepository repository;

    @Override
    @Transactional
    public void saveAllocationGoals(String userId, List<AllocationGoalDto> goals) {
        repository.deleteByUserId(userId);

        if (goals == null || goals.isEmpty()) {
            return;
        }

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        List<AllocationGoalEntity> entities = goals.stream()
            .map(dto -> AllocationGoalEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(dto.goalType())
                .targetKey(dto.targetKey())
                .targetPercentage(dto.targetPercentage())
                .createdAt(nowUtc)
                .updatedAt(nowUtc)
                .build())
            .toList();

        repository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationGoalDto> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
            .map(entity -> new AllocationGoalDto(
                entity.getGoalType(),
                entity.getTargetKey(),
                entity.getTargetPercentage()
            ))
            .toList();
    }
}
