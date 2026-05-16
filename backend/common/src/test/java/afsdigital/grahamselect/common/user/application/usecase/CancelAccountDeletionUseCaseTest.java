package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionNotFoundException;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelAccountDeletionUseCaseTest {

    @Mock
    private AccountDeletionRequestRepository repository;

    @InjectMocks
    private CancelAccountDeletionUseCase useCase;

    private final Long userId = 1L;

    @Test
    void shouldCancelRequestWhenPending() {
        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .userId(userId)
                .status(DeletionStatus.PENDING)
                .build();

        when(repository.findPendingByUserId(userId)).thenReturn(Optional.of(request));

        useCase.execute(userId);

        assertEquals(DeletionStatus.CANCELLED, request.getStatus());
        verify(repository).save(request);
    }

    @Test
    void shouldThrowExceptionWhenNoRequestPending() {
        when(repository.findPendingByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(DeletionNotFoundException.class, () -> useCase.execute(userId));
        verify(repository, never()).save(any());
    }
}
