package afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities.OpenFinanceSyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OpenFinanceSyncLogJpaRepository extends JpaRepository<OpenFinanceSyncLogEntity, UUID> {

    List<OpenFinanceSyncLogEntity> findByConnectionIdOrderByStartedAtDesc(UUID connectionId);

    List<OpenFinanceSyncLogEntity> findByUserIdOrderByStartedAtDesc(String userId);
}
