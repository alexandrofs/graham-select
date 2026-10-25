package afsdigital.grahamselect.api.dlq.application.dto;

import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;

public record DlqReprocessResultDto(
        Long id,
        DlqMessageStatus status,
        String message
) {
}
