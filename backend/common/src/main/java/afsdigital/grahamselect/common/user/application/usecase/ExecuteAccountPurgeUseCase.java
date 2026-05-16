package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import afsdigital.grahamselect.common.user.application.repository.UserDeletionPort;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
public class ExecuteAccountPurgeUseCase {

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final UserDataPurgePort userDataPurgePort;
    private final UserDeletionPort userDeletionPort;

    public void execute(AccountDeletionRequest request) {
        Long userId = request.getUserId();
        log.info("Starting purge for user ID: {}", userId);

        // 1. Purge data from other modules
        userDataPurgePort.purgeAllUserData(userId);

        // 2. Delete user record (FK ON DELETE SET NULL handles user_id in requests)
        userDeletionPort.deleteById(userId);

        // 3. Update request status
        request.setStatus(DeletionStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        request.setUserId(null); // Match DB state after FK cascade
        deletionRequestRepository.save(request);

        log.info("Purge completed for user ID: {}", userId);
    }
}
