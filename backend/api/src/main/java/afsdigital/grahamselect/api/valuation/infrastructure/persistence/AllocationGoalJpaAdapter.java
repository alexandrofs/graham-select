package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repositories.AllocationGoalJpaRepository;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationGoalJpaAdapter implements AllocationGoalPort {

    private final AllocationGoalJpaRepository repository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void saveAllocationGoals(String userId, List<AllocationGoalDto> goals) {
        saveAllocationGoals(userId, null, goals);
    }

    @Override
    @Transactional
    public void saveAllocationGoals(String userId, UUID tagId, List<AllocationGoalDto> goals) {
        userRepository.findByGoogleSubForWrite(userId)
            .orElseThrow(() -> {
                IllegalArgumentException ex = new IllegalArgumentException("User not found: " + userId);
                log.error("User not found while saving allocation goals. userId={}, tagId={}", userId, tagId, ex);
                return ex;
            });
        String scopeKey = AllocationScope.fromTagId(tagId);
        repository.deleteByUserIdAndScopeKey(userId, scopeKey);

        if (goals == null || goals.isEmpty()) {
            return;
        }

        List<AllocationGoalDto> uniqueGoals = goals.stream()
            .collect(java.util.stream.Collectors.toMap(
                dto -> dto.goalType() + "#" + dto.targetKey(),
                dto -> dto,
                (existing, replacement) -> replacement
            ))
            .values()
            .stream()
            .toList();

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        List<AllocationGoalEntity> entities = uniqueGoals.stream()
            .map(dto -> AllocationGoalEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tagId(tagId)
                .scopeKey(scopeKey)
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
        return findByUserId(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationGoalDto> findByUserId(String userId, UUID tagId) {
        String scopeKey = AllocationScope.fromTagId(tagId);
        return repository.findByUserIdAndScopeKeyOrderByGoalTypeAscTargetKeyAsc(userId, scopeKey).stream()
            .map(entity -> new AllocationGoalDto(
                entity.getGoalType(),
                entity.getTargetKey(),
                entity.getTargetPercentage()
            ))
            .toList();
    }
}
