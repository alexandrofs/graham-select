package afsdigital.grahamselect.common.openfinance.application.repository;

import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpenFinanceConnectionPort {
    OpenFinanceConnection save(OpenFinanceConnection connection);

    Optional<OpenFinanceConnection> findById(UUID id);

    Optional<OpenFinanceConnection> findByIdAndUserId(UUID id, String userId);

    Optional<OpenFinanceConnection> findByUserIdAndInstitutionIdAndProfileId(String userId, String institutionId, UUID profileId);

    List<OpenFinanceConnection> findByUserId(String userId);

    List<OpenFinanceConnection> findActiveConnections();

    void deleteById(UUID id);
}
