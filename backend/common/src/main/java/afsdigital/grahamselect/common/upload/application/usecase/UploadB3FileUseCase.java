package afsdigital.grahamselect.common.upload.application.usecase;

import afsdigital.grahamselect.common.upload.application.repository.B3FileStoragePort;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.application.repository.B3UploadEventPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RequiredArgsConstructor
public class UploadB3FileUseCase {

    private final B3FileStoragePort storagePort;
    private final B3UploadEventPort eventPort;
    private final B3ImportStatusPort importStatusPort;

    public FileUploadedEvent execute(InputStream inputStream, String fileName, String userId) {
        String storagePath = storagePort.save(inputStream, fileName, userId);
        String correlationId = UUID.randomUUID().toString();

        FileUploadedEvent event = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .fileName(fileName)
                .storagePath(storagePath)
                .correlationId(correlationId)
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        importStatusPort.createPending(correlationId, userId, fileName);
        eventPort.publish(event);
        return event;
    }
}
