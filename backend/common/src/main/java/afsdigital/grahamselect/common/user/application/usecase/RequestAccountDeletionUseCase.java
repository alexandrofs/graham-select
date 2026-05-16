package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionAlreadyPendingException;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RequiredArgsConstructor
public class RequestAccountDeletionUseCase {

    private final AccountDeletionRequestRepository repository;

    public AccountDeletionRequest execute(Long userId) {
        repository.findPendingByUserId(userId).ifPresent(r -> {
            throw new DeletionAlreadyPendingException("User already has a pending deletion request");
        });

        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .userId(userId)
                .status(DeletionStatus.PENDING)
                .requestedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        return repository.save(request);
    }
}
