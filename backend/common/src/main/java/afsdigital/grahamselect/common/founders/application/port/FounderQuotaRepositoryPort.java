package afsdigital.grahamselect.common.founders.application.port;

import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;

import java.time.LocalDateTime;

public interface FounderQuotaRepositoryPort {

    FounderSlotsStatus getStatus();

    boolean tryClaimFounderSlot(LocalDateTime nowUtc);
}
