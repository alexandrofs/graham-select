package afsdigital.grahamselect.common.ingestion.application.service;

import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit.IngestionStatus;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories.IngestionAuditRepository;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final IngestionAuditRepository repository;

    @Transactional
    public void createAudit(FileUploadedEvent event) {
        IngestionAudit audit = IngestionAudit.builder()
                .userId(event.userId())
                .correlationId(event.correlationId())
                .fileName(event.fileName())
                .uploadDate(event.timestamp().toLocalDateTime())
                .status(IngestionStatus.PROCESSANDO)
                .totalLines(0)
                .processedLines(0)
                .errorLines(0)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        repository.save(audit);
    }

    @Transactional
    public void updateAuditProgress(String correlationId, int processed, int errors, boolean completed) {
        repository.findByCorrelationId(correlationId)
                .ifPresent(audit -> {
                    audit.setProcessedLines(audit.getProcessedLines() + processed);
                    audit.setErrorLines(audit.getErrorLines() + errors);
                    if (completed) {
                        if (audit.getErrorLines() > 0) {
                            audit.setStatus(IngestionStatus.PARCIAL);
                        } else {
                            audit.setStatus(IngestionStatus.SUCESSO);
                        }
                    }
                    repository.save(audit);
                });
    }

    @Transactional
    public void markAsError(String correlationId) {
        repository.findByCorrelationId(correlationId)
                .ifPresent(audit -> {
                    audit.setStatus(IngestionStatus.ERRO);
                    repository.save(audit);
                });
    }
}
