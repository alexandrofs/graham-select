package afsdigital.grahamselect.common.portfolio.application.repository;

import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;

public interface ManualTradeAuditPort {
    void save(ManualTradeAudit audit);
}
