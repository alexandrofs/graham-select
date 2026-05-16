package afsdigital.grahamselect.common.user.application.repository;

import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;

import java.util.List;
import java.util.Optional;

public interface AccountDeletionRequestRepository {
    AccountDeletionRequest save(AccountDeletionRequest request);
    Optional<AccountDeletionRequest> findById(Long id);
    Optional<AccountDeletionRequest> findPendingByUserId(Long userId);
    List<AccountDeletionRequest> findByStatus(DeletionStatus status);
}
