package afsdigital.grahamselect.common.upload.application.repository;

import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;

import java.util.Optional;

public interface B3ImportStatusPort {

    B3ImportStatus createPending(String correlationId, String userId, String fileName);

    B3ImportStatus markProcessing(String correlationId, String userId);

    void addProgress(String correlationId, String userId, int addedSuccess, int addedFailure, String lastFailureReason);

    void addDuplicate(String correlationId, String userId, int addedDuplicate);

    B3ImportStatus markCompleted(String correlationId, String userId);

    B3ImportStatus markFailed(String correlationId, String userId, String failureReason);

    Optional<B3ImportStatus> findLatestByUserId(String userId);

    Optional<B3ImportStatus> findByCorrelationIdAndUserId(String correlationId, String userId);
}
