package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import afsdigital.grahamselect.common.user.application.repository.UserDeletionPort;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecuteAccountPurgeUseCaseTest {

    @Mock
    private AccountDeletionRequestRepository deletionRequestRepository;
    @Mock
    private UserDataPurgePort userDataPurgePort;
    @Mock
    private UserDeletionPort userDeletionPort;

    @InjectMocks
    private ExecuteAccountPurgeUseCase useCase;

    @Test
    void shouldExecutePurgeSuccessfully() {
        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .id(1L)
                .userId(1L)
                .status(DeletionStatus.PENDING)
                .build();

        useCase.execute(request);

        assertEquals(DeletionStatus.COMPLETED, request.getStatus());
        assertNotNull(request.getCompletedAt());
        assertNull(request.getUserId());
        verify(userDataPurgePort).purgeAllUserData(1L);
        verify(userDeletionPort).deleteById(1L);
        verify(deletionRequestRepository).save(request);
    }

    @Test
    void shouldPropagateExceptionWhenPurgeFails() {
        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .id(1L)
                .userId(1L)
                .status(DeletionStatus.PENDING)
                .build();

        doThrow(new RuntimeException("Database error")).when(userDataPurgePort).purgeAllUserData(1L);

        assertThrows(RuntimeException.class, () -> useCase.execute(request));

        // Status should NOT be changed by the use case - scheduler handles failure marking
        assertEquals(DeletionStatus.PENDING, request.getStatus());
        verify(userDeletionPort, never()).deleteById(anyLong());
        verify(deletionRequestRepository, never()).save(any());
    }
}
