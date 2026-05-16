package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
public class ExecuteAccountPurgeUseCase {

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final UserDataPurgePort userDataPurgePort;
    private final UserRepository userRepository;

    public void execute(AccountDeletionRequest request) {
        Long userId = request.getUserId();
        try {
            log.info("Starting purge for user ID: {}", userId);
            
            // 1. Purge data from other modules
            userDataPurgePort.purgeAllUserData(userId);
            
            // 2. Update request status and clear user_id to allow user deletion
            request.setStatus(DeletionStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
            request.setUserId(null); // Disconnect from user to allow deletion
            deletionRequestRepository.save(request);
            
            // 3. Delete user record
            userRepository.deleteById(userId);
            
            log.info("Purge completed for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to purge data for user ID: {}", userId, e);
            request.setStatus(DeletionStatus.FAILED);
            request.setFailureReason(e.getMessage());
            deletionRequestRepository.save(request);
            throw e;
        }
    }
}
