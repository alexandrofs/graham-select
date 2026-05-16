package afsdigital.grahamselect.common.upload.application.usecase;

import afsdigital.grahamselect.common.upload.application.repository.B3FileStoragePort;
import afsdigital.grahamselect.common.upload.application.repository.B3UploadEventPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UploadB3FileUseCaseTest {

    @Mock
    private B3FileStoragePort storagePort;

    @Mock
    private B3UploadEventPort eventPort;

    @InjectMocks
    private UploadB3FileUseCase useCase;

    @Test
    public void shouldSaveFileAndEmitEvent() {
        InputStream fileStream = mock(InputStream.class);
        String userId = "user-123";
        String filename = "negociacao.xlsx";
        String savedPath = "/tmp/uploads/negociacao.xlsx";

        when(storagePort.save(any(), eq(filename), eq(userId))).thenReturn(savedPath);

        useCase.execute(fileStream, filename, userId);

        verify(storagePort).save(fileStream, filename, userId);
        verify(eventPort).publish(argThat(event -> 
            event.userId().equals(userId) && 
            event.fileName().equals(filename) && 
            event.storagePath().equals(savedPath) &&
            event.correlationId() != null
        ));
    }
}
