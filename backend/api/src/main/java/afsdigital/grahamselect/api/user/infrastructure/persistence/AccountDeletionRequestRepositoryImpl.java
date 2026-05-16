package afsdigital.grahamselect.api.user.infrastructure.persistence;

import afsdigital.grahamselect.api.user.infrastructure.persistence.jpa.entities.AccountDeletionRequestEntity;
import afsdigital.grahamselect.api.user.infrastructure.persistence.jpa.repository.AccountDeletionRequestJpaRepository;
import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AccountDeletionRequestRepositoryImpl implements AccountDeletionRequestRepository {

    private final AccountDeletionRequestJpaRepository jpaRepository;

    @Override
    public AccountDeletionRequest save(AccountDeletionRequest request) {
        AccountDeletionRequestEntity entity = toEntity(request);
        AccountDeletionRequestEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<AccountDeletionRequest> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AccountDeletionRequest> findPendingByUserId(Long userId) {
        return jpaRepository.findByUserIdAndStatus(userId, DeletionStatus.PENDING).map(this::toDomain);
    }

    @Override
    public List<AccountDeletionRequest> findByStatus(DeletionStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private AccountDeletionRequestEntity toEntity(AccountDeletionRequest domain) {
        return AccountDeletionRequestEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .status(domain.getStatus())
                .requestedAt(domain.getRequestedAt())
                .completedAt(domain.getCompletedAt())
                .failureReason(domain.getFailureReason())
                .build();
    }

    private AccountDeletionRequest toDomain(AccountDeletionRequestEntity entity) {
        return AccountDeletionRequest.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .completedAt(entity.getCompletedAt())
                .failureReason(entity.getFailureReason())
                .build();
    }
}
