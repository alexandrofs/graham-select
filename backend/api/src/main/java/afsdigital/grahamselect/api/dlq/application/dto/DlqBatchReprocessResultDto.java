package afsdigital.grahamselect.api.dlq.application.dto;

import java.util.List;

public record DlqBatchReprocessResultDto(
        int totalProcessed,
        int successCount,
        int failureCount,
        List<DlqReprocessResultDto> details
) {
}
