package afsdigital.grahamselect.api.upload.web;

public record B3UploadAcceptedResponse(
        String message,
        String correlationId,
        String status
) {
}
