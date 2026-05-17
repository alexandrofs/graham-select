package afsdigital.grahamselect.api.upload.web;

public record B3ImportStatusView(
        String correlationId,
        String fileName,
        String status,
        int processedRows,
        int successfulRows,
        int failedRows,
        String message
) {
}
