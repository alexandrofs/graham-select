package afsdigital.grahamselect.api.user.infrastructure.spring;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.usecase.ExecuteAccountPurgeUseCase;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPurgeScheduler {

    private final AccountDeletionRequestRepository repository;
    private final ExecuteAccountPurgeUseCase purgeUseCase;

    @Scheduled(fixedRate = 3600000) // 1h
    public void processPendingPurges() {
        log.info("Checking for pending account deletion requests...");
        List<AccountDeletionRequest> pendingRequests = repository.findByStatus(DeletionStatus.PENDING);
        
        if (pendingRequests.isEmpty()) {
            log.info("No pending deletion requests found.");
            return;
        }

        log.info("Found {} pending deletion requests. Starting purge process...", pendingRequests.size());
        
        for (AccountDeletionRequest request : pendingRequests) {
            try {
                purgeUseCase.execute(request);
            } catch (Exception e) {
                log.error("Failed to process purge request ID: {} for user ID: {}", request.getId(), request.getUserId(), e);
            }
        }
    }
}
