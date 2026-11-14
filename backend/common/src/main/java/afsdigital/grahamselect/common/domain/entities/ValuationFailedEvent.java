package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValuationFailedEvent(
    String userId,
    String errorCode,
    String message,
    String failedAt,
    UUID eventId,
    UUID tagId
) {
    public ValuationFailedEvent(String userId, String errorCode, String message, String failedAt, UUID eventId) {
        this(userId, errorCode, message, failedAt, eventId, null);
    }
}
