package afsdigital.grahamselect.common.upload.application.usecase;

import afsdigital.grahamselect.common.upload.application.repository.B3FileStoragePort;
import afsdigital.grahamselect.common.upload.application.repository.B3UploadEventPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadB3FileUseCase {

    private final B3FileStoragePort storagePort;
    private final B3UploadEventPort eventPort;

    public void execute(InputStream inputStream, String fileName, String userId) {
        String storagePath = storagePort.save(inputStream, fileName, userId);

        FileUploadedEvent event = FileUploadedEvent.builder()
                .userId(userId)
                .fileName(fileName)
                .storagePath(storagePath)
                .correlationId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        eventPort.publish(event);
    }
}
