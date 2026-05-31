package afsdigital.grahamselect.common.domain.repository;

import afsdigital.grahamselect.common.domain.entities.StockPrice;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockPricePort {
    Optional<StockPrice> findLatestByCompanyId(String companyId);
    Map<String, StockPrice> findLatestByCompanyIds(List<String> companyIds);
}
