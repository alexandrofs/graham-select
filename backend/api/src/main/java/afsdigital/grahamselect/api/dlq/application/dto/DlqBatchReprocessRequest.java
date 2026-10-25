package afsdigital.grahamselect.api.dlq.application.dto;

import java.util.List;

public record DlqBatchReprocessRequest(
        List<Long> ids
) {
}
