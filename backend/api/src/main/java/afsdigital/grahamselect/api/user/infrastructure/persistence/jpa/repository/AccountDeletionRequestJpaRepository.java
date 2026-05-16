package afsdigital.grahamselect.api.user.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.user.infrastructure.persistence.jpa.entities.AccountDeletionRequestEntity;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountDeletionRequestJpaRepository extends JpaRepository<AccountDeletionRequestEntity, Long> {
    Optional<AccountDeletionRequestEntity> findByUserIdAndStatus(Long userId, DeletionStatus status);
    List<AccountDeletionRequestEntity> findByStatus(DeletionStatus status);
}
