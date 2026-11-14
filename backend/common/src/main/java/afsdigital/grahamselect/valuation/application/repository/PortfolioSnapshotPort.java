package afsdigital.grahamselect.valuation.application.repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PortfolioSnapshotPort {
    Map<String, BigDecimal> getCurrentAllocationByUserId(String userId);

    default Map<String, BigDecimal> getCurrentAllocationByUserIdAndTagId(String userId, UUID tagId) {
        return getCurrentAllocationByUserId(userId);
    }
}
