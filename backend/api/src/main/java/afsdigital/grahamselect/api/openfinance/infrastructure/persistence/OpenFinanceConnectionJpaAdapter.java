package afsdigital.grahamselect.api.openfinance.infrastructure.persistence;

import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities.OpenFinanceConnectionEntity;
import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.repositories.OpenFinanceConnectionJpaRepository;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenFinanceConnectionJpaAdapter implements OpenFinanceConnectionPort {

    private final OpenFinanceConnectionJpaRepository repository;

    @Override
    public OpenFinanceConnection save(OpenFinanceConnection connection) {
        OpenFinanceConnectionEntity entity = OpenFinanceConnectionEntity.fromDomain(connection);
        // If entity already exists in database, mark isNew as false so Spring Data merges it
        if (connection.getId() != null && repository.existsById(connection.getId())) {
            entity.setNew(false);
        }
        OpenFinanceConnectionEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<OpenFinanceConnection> findById(UUID id) {
        return repository.findById(id).map(OpenFinanceConnectionEntity::toDomain);
    }

    @Override
    public Optional<OpenFinanceConnection> findByIdAndUserId(UUID id, String userId) {
        return repository.findByIdAndUserId(id, userId).map(OpenFinanceConnectionEntity::toDomain);
    }

    @Override
    public Optional<OpenFinanceConnection> findByUserIdAndInstitutionIdAndProfileId(String userId, String institutionId, UUID profileId) {
        return repository.findByUserIdAndInstitutionIdAndProfileId(userId, institutionId, profileId)
                .map(OpenFinanceConnectionEntity::toDomain);
    }

    @Override
    public List<OpenFinanceConnection> findByUserId(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OpenFinanceConnectionEntity::toDomain)
                .toList();
    }

    @Override
    public List<OpenFinanceConnection> findActiveConnections() {
        return repository.findActiveNotExpired(OpenFinanceConnectionStatus.ACTIVE.name(), LocalDateTime.now(ZoneOffset.UTC)).stream()
                .map(OpenFinanceConnectionEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
