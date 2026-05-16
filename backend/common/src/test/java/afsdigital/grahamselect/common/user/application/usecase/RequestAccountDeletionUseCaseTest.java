package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionAlreadyPendingException;
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
class RequestAccountDeletionUseCaseTest {

    @Mock
    private AccountDeletionRequestRepository repository;

    @InjectMocks
    private RequestAccountDeletionUseCase useCase;

    private final Long userId = 1L;

    @Test
    void shouldCreateRequestWhenNonePending() {
        when(repository.findPendingByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any(AccountDeletionRequest.class))).thenAnswer(i -> i.getArgument(0));

        AccountDeletionRequest result = useCase.execute(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(DeletionStatus.PENDING, result.getStatus());
        assertNotNull(result.getRequestedAt());
        verify(repository).save(any(AccountDeletionRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenRequestAlreadyPending() {
        when(repository.findPendingByUserId(userId)).thenReturn(Optional.of(new AccountDeletionRequest()));

        assertThrows(DeletionAlreadyPendingException.class, () -> useCase.execute(userId));
        verify(repository, never()).save(any());
    }
}
