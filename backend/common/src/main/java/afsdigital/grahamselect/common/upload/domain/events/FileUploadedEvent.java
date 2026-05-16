package afsdigital.grahamselect.common.upload.domain.events;

import lombok.Builder;
import java.time.OffsetDateTime;

@Builder
public record FileUploadedEvent(
    String userId,
    String fileName,
    String storagePath,
    String correlationId,
    OffsetDateTime timestamp
) {}
