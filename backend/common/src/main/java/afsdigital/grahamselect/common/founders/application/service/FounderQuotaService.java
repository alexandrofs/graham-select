package afsdigital.grahamselect.common.founders.application.service;

import afsdigital.grahamselect.common.founders.application.port.FounderQuotaRepositoryPort;
import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FounderQuotaService {

    private final FounderQuotaRepositoryPort repositoryPort;
    private final int trialDays;

    public FounderQuotaService(FounderQuotaRepositoryPort repositoryPort, int trialDays) {
        this.repositoryPort = repositoryPort;
        this.trialDays = trialDays;
    }

    public FounderSlotsStatus getSlotsStatus() {
        return repositoryPort.getStatus();
    }

    public boolean tryClaimFounderSlot() {
        return repositoryPort.tryClaimFounderSlot(LocalDateTime.now(ZoneOffset.UTC));
    }

    public LocalDateTime calculateProTrialExpiration(LocalDateTime nowUtc) {
        return nowUtc.plusDays(trialDays);
    }

    public int getTrialDays() {
        return trialDays;
    }
}
