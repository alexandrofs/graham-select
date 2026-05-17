package afsdigital.grahamselect.api.ingestion.web;

import afsdigital.grahamselect.common.ingestion.application.usecase.GetIngestionAuditHistoryUseCase;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ingestion/history")
@RequiredArgsConstructor
public class IngestionAuditController {

    private final GetIngestionAuditHistoryUseCase getHistoryUseCase;

    @GetMapping
    public ResponseEntity<List<IngestionAuditResponse>> getHistory(@AuthenticationPrincipal Jwt jwt) {
        List<IngestionAudit> history = getHistoryUseCase.execute(jwt.getSubject());
        List<IngestionAuditResponse> response = history.stream()
                .map(audit -> new IngestionAuditResponse(
                        audit.getFileName(),
                        audit.getUploadDate(),
                        audit.getStatus().name(),
                        audit.getProcessedLines(),
                        audit.getErrorLines()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record IngestionAuditResponse(
            String fileName,
            LocalDateTime uploadDate,
            String status,
            int successfulLines,
            int errorLines
    ) {}
}
