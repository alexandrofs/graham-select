package afsdigital.grahamselect.common.upload.domain.events;

import lombok.Builder;
import java.time.OffsetDateTime;

@Builder
public record FileUploadedEvent(
    String eventId,
    String userId,
    String fileName,
    String storagePath,
    String correlationId,
    String version,
    OffsetDateTime timestamp
) {}
