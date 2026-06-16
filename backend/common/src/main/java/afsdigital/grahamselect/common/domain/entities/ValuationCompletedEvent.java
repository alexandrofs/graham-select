package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ValuationCompletedEvent(
    String userId,
    int recommendationCount,
    List<ExcludedTicker> excludedTickers,
    String completedAt,
    UUID eventId
) {
    public static final int STALE_DATA_THRESHOLD_DAYS = 7;
    public static final BigDecimal OUTLIER_MARGIN_THRESHOLD = new BigDecimal("0.80");
}
