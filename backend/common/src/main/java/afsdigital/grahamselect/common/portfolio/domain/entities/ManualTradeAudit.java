package afsdigital.grahamselect.common.portfolio.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ManualTradeAudit {
    private UUID id;
    private UUID tradeId;
    private String userId;
    private String actionType;
    private String payloadJson;
    private LocalDateTime createdAt;
}
