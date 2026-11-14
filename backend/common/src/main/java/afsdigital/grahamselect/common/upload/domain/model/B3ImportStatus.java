package afsdigital.grahamselect.common.upload.domain.model;

import java.time.LocalDateTime;

public record B3ImportStatus(
        String correlationId,
        String userId,
        String profileId,
        String fileName,
        B3ImportStatusState status,
        int processedRows,
        int successfulRows,
        int failedRows,
        int duplicatedRows,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public B3ImportStatus(
            String correlationId,
            String userId,
            String fileName,
            B3ImportStatusState status,
            int processedRows,
            int successfulRows,
            int failedRows,
            int duplicatedRows,
            String message,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime completedAt
    ) {
        this(correlationId, userId, null, fileName, status, processedRows, successfulRows, failedRows, duplicatedRows, message, createdAt, updatedAt, completedAt);
    }
}
