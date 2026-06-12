package afsdigital.grahamselect.valuation.application.repository;

import java.math.BigDecimal;
import java.util.Map;

public interface PortfolioSnapshotPort {
    Map<String, BigDecimal> getCurrentAllocationByUserId(String userId);
}
