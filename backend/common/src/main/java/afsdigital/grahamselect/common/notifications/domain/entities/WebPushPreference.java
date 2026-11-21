package afsdigital.grahamselect.common.notifications.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebPushPreference {
    private UUID id;
    private String userId;
    private boolean optedIn;
    private OffsetDateTime optedInAt;
    private BigDecimal minMarginOfSafetyPct;
    private boolean onlyCustodyTickers;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
