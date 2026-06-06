package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.ManualTradeAuditEntity;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaManualTradeAuditRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.domain.entities.ManualTradeAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
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
        try {
            repository.save(entity);
        } catch (Exception e) {
            log.error("Database error when persisting manual trade audit id: {} for tradeId: {}", entity.getId(), entity.getTradeId(), e);
            throw e;
        }
    }
}
