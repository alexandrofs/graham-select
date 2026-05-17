package afsdigital.grahamselect.common.upload.domain.model;

import java.util.Map;

public record B3TradeRowFailure(
        long lineNumber,
        Map<String, String> originalPayload,
        String reason
) {
}
