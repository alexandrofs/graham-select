package afsdigital.grahamselect.api.upload.infrastructure.persistence;

import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.entities.B3ImportStatusEntity;
import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.repository.B3ImportStatusJpaRepository;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatusState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
@Transactional
@RequiredArgsConstructor
public class B3ImportStatusRepositoryAdapter implements B3ImportStatusPort {

    private final B3ImportStatusJpaRepository repository;

    @Override
    public B3ImportStatus createPending(String correlationId, String userId, String fileName) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        B3ImportStatusEntity entity = B3ImportStatusEntity.builder()
                .correlationId(correlationId)
                .userId(userId)
                .fileName(fileName)
                .status(B3ImportStatusState.RECEIVED)
                .processedRows(0)
                .successfulRows(0)
                .failedRows(0)
                .message("Arquivo recebido e aguardando processamento.")
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toDomain(repository.save(entity));
    }

    @Override
    public B3ImportStatus markProcessing(String correlationId, String userId) {
        B3ImportStatusEntity entity = getByCorrelationIdAndUserId(correlationId, userId);
        entity.setStatus(B3ImportStatusState.PROCESSING);
        entity.setProcessedRows(0);
        entity.setSuccessfulRows(0);
        entity.setFailedRows(0);
        entity.setMessage("Processando arquivo em segundo plano.");
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return toDomain(repository.save(entity));
    }

    @Override
    public void addProgress(String correlationId, String userId, int addedSuccess, int addedFailure, String lastFailureReason) {
        B3ImportStatusEntity entity = getByCorrelationIdAndUserId(correlationId, userId);
        entity.setProcessedRows(entity.getProcessedRows() + addedSuccess + addedFailure);
        entity.setSuccessfulRows(entity.getSuccessfulRows() + addedSuccess);
        entity.setFailedRows(entity.getFailedRows() + addedFailure);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        String msg = buildProgressMessage(entity);
        if (lastFailureReason != null && !lastFailureReason.isBlank()) {
            msg += " Última falha: " + lastFailureReason;
        }
        entity.setMessage(msg);
        repository.save(entity);
    }

    @Override
    public B3ImportStatus markCompleted(String correlationId, String userId) {
        B3ImportStatusEntity entity = getByCorrelationIdAndUserId(correlationId, userId);
        entity.setStatus(entity.getFailedRows() > 0
                ? B3ImportStatusState.COMPLETED_WITH_ERRORS
                : B3ImportStatusState.COMPLETED);
        entity.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(entity.getCompletedAt());
        entity.setMessage(entity.getFailedRows() > 0
                ? String.format(
                "Processamento concluído com alertas: %d sucesso(s), %d falha(s).",
                entity.getSuccessfulRows(),
                entity.getFailedRows())
                : String.format(
                "Processamento concluído com sucesso: %d linha(s) processada(s).",
                entity.getSuccessfulRows()));
        return toDomain(repository.save(entity));
    }

    @Override
    public B3ImportStatus markFailed(String correlationId, String userId, String failureReason) {
        B3ImportStatusEntity entity = getByCorrelationIdAndUserId(correlationId, userId);
        entity.setStatus(B3ImportStatusState.FAILED);
        entity.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(entity.getCompletedAt());
        entity.setMessage(failureReason);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<B3ImportStatus> findLatestByUserId(String userId) {
        return repository.findTopByUserIdOrderByCreatedAtDesc(userId).map(this::toDomain);
    }

    @Override
    public Optional<B3ImportStatus> findByCorrelationIdAndUserId(String correlationId, String userId) {
        return repository.findByCorrelationIdAndUserId(correlationId, userId).map(this::toDomain);
    }

    private B3ImportStatusEntity getByCorrelationIdAndUserId(String correlationId, String userId) {
        return repository.findByCorrelationIdAndUserId(correlationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Status de importação não encontrado para " + correlationId));
    }

    private String buildProgressMessage(B3ImportStatusEntity entity) {
        return String.format(
                "Processadas %d linha(s): %d sucesso(s), %d falha(s).",
                entity.getProcessedRows(),
                entity.getSuccessfulRows(),
                entity.getFailedRows());
    }

    private B3ImportStatus toDomain(B3ImportStatusEntity entity) {
        return new B3ImportStatus(
                entity.getCorrelationId(),
                entity.getUserId(),
                entity.getFileName(),
                entity.getStatus(),
                entity.getProcessedRows(),
                entity.getSuccessfulRows(),
                entity.getFailedRows(),
                entity.getMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCompletedAt()
        );
    }
}
