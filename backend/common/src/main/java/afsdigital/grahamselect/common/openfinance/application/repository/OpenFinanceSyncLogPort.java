package afsdigital.grahamselect.common.openfinance.application.repository;

import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncLog;

import java.util.List;
import java.util.UUID;

public interface OpenFinanceSyncLogPort {
    OpenFinanceSyncLog save(OpenFinanceSyncLog syncLog);

    List<OpenFinanceSyncLog> findByConnectionIdOrderByStartedAtDesc(UUID connectionId);

    List<OpenFinanceSyncLog> findByUserIdOrderByStartedAtDesc(String userId);
}
