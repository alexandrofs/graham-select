package afsdigital.grahamselect.common.ingestion.application.usecase;

import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories.IngestionAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetIngestionAuditHistoryUseCase {

    private final IngestionAuditRepository repository;

    public List<IngestionAudit> execute(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required for ingestion audit history");
        }
        return repository.findAllByUserIdOrderByUploadDateDesc(userId);
    }
}
