package afsdigital.grahamselect.api.dlq.application.dto;

import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;

import java.time.OffsetDateTime;

public record DlqMessageDto(
        Long id,
        String eventId,
        String correlationId,
        String anonymizedUserId,
        String fileName,
        Long lineNumber,
        String errorMessage,
        String originalTopic,
        DlqMessageStatus status,
        OffsetDateTime timestamp,
        OffsetDateTime resolvedAt
) {
}
