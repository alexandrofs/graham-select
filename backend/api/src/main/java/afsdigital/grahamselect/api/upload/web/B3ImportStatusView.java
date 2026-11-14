package afsdigital.grahamselect.api.upload.web;

public record B3ImportStatusView(
        String correlationId,
        String fileName,
        String profileId,
        String status,
        int processedRows,
        int successfulRows,
        int failedRows,
        int duplicatedRows,
        String message
) {
    public B3ImportStatusView(
            String correlationId,
            String fileName,
            String status,
            int processedRows,
            int successfulRows,
            int failedRows,
            int duplicatedRows,
            String message
    ) {
        this(correlationId, fileName, null, status, processedRows, successfulRows, failedRows, duplicatedRows, message);
    }
}
