package afsdigital.grahamselect.api.admin.application.dto;

import java.time.OffsetDateTime;

public record AnonymizedUploadTraceDto(
        String anonymizedUserId,
        String sanitizedFileName,
        String status,
        int processedLines,
        int errorLines,
        String maskedPortfolioValue,
        OffsetDateTime uploadTimestamp
) {
}
