package afsdigital.grahamselect.common.upload.domain.model;

import java.time.LocalDateTime;

public record B3ImportStatus(
        String correlationId,
        String userId,
        String fileName,
        B3ImportStatusState status,
        int processedRows,
        int successfulRows,
        int failedRows,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
