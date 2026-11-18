package afsdigital.grahamselect.api.openfinance.infrastructure.persistence;

import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities.OpenFinanceSyncLogEntity;
import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.repositories.OpenFinanceSyncLogJpaRepository;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceSyncLogPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenFinanceSyncLogJpaAdapter implements OpenFinanceSyncLogPort {

    private final OpenFinanceSyncLogJpaRepository repository;

    @Override
    public OpenFinanceSyncLog save(OpenFinanceSyncLog syncLog) {
        OpenFinanceSyncLogEntity entity = OpenFinanceSyncLogEntity.fromDomain(syncLog);
        if (syncLog.getId() != null && repository.existsById(syncLog.getId())) {
            entity.setNew(false);
        }
        OpenFinanceSyncLogEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<OpenFinanceSyncLog> findByConnectionIdOrderByStartedAtDesc(UUID connectionId) {
        return repository.findByConnectionIdOrderByStartedAtDesc(connectionId).stream()
                .map(OpenFinanceSyncLogEntity::toDomain)
                .toList();
    }

    @Override
    public List<OpenFinanceSyncLog> findByUserIdOrderByStartedAtDesc(String userId) {
        return repository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(OpenFinanceSyncLogEntity::toDomain)
                .toList();
    }
}
