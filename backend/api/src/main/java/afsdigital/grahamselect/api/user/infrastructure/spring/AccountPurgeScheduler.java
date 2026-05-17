package afsdigital.grahamselect.api.user.infrastructure.spring;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.usecase.ExecuteAccountPurgeUseCase;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Component
public class AccountPurgeScheduler {

    private final AccountDeletionRequestRepository repository;
    private final ExecuteAccountPurgeUseCase purgeUseCase;
    private final TransactionTemplate transactionTemplate;

    public AccountPurgeScheduler(AccountDeletionRequestRepository repository, ExecuteAccountPurgeUseCase purgeUseCase, @org.springframework.beans.factory.annotation.Qualifier("transactionManager") org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.purgeUseCase = purgeUseCase;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

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
            Long requestId = request.getId();
            Long userId = request.getUserId();
            try {
                transactionTemplate.executeWithoutResult(status ->
                        purgeUseCase.execute(request)
                );
            } catch (Exception e) {
                log.error("Failed to process purge request ID: {} for user ID: {}", requestId, userId, e);
                markAsFailed(request, e);
            }
        }
    }

    private void markAsFailed(AccountDeletionRequest request, Exception e) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                request.setStatus(DeletionStatus.FAILED);
                request.setFailureReason(e.getMessage());
                repository.save(request);
            });
        } catch (Exception ex) {
            log.error("Failed to mark request ID: {} as FAILED", request.getId(), ex);
        }
    }
}
