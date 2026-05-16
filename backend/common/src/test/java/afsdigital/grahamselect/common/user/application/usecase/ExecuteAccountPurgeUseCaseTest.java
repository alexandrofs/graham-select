package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecuteAccountPurgeUseCaseTest {

    @Mock
    private AccountDeletionRequestRepository deletionRequestRepository;
    @Mock
    private UserDataPurgePort userDataPurgePort;
    @Mock
    private UserRepository userRepository;

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
        verify(userDataPurgePort).purgeAllUserData(1L);
        verify(userRepository).deleteById(1L);
        verify(deletionRequestRepository).save(request);
    }

    @Test
    void shouldMarkAsFailedWhenErrorOccurs() {
        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .id(1L)
                .userId(1L)
                .status(DeletionStatus.PENDING)
                .build();

        doThrow(new RuntimeException("Database error")).when(userDataPurgePort).purgeAllUserData(1L);

        assertThrows(RuntimeException.class, () -> useCase.execute(request));

        assertEquals(DeletionStatus.FAILED, request.getStatus());
        assertEquals("Database error", request.getFailureReason());
        verify(deletionRequestRepository).save(request);
    }
}
