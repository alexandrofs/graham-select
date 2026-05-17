package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.ManualTradeAuditEntity;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaManualTradeAuditRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManualTradeAuditJpaAdapter implements ManualTradeAuditPort {

    private final JpaManualTradeAuditRepository repository;

    @Override
    public void save(ManualTradeAudit audit) {
        ManualTradeAuditEntity entity = ManualTradeAuditEntity.builder()
                .id(audit.getId())
                .tradeId(audit.getTradeId())
                .userId(audit.getUserId())
                .actionType(audit.getActionType())
                .payloadJson(audit.getPayloadJson())
                .createdAt(audit.getCreatedAt())
                .build();
        repository.save(entity);
    }
}
