package afsdigital.grahamselect.common.upload.domain.events;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import java.time.OffsetDateTime;

@Builder
@Jacksonized
public record FileUploadedEvent(
    String eventId,
    String userId,
    String fileName,
    String storagePath,
    String correlationId,
    String version,
    OffsetDateTime timestamp
) {}
