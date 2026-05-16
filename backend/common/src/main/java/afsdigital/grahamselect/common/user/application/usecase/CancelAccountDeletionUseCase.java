package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionNotFoundException;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelAccountDeletionUseCase {

    private final AccountDeletionRequestRepository repository;

    public void execute(Long userId) {
        AccountDeletionRequest request = repository.findPendingByUserId(userId)
                .orElseThrow(() -> new DeletionNotFoundException("No pending deletion request found for user"));

        request.setStatus(DeletionStatus.CANCELLED);
        repository.save(request);
    }
}
