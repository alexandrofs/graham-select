package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.valuation.domain.entities.StockPrice;

public interface ValuationRepository {
    void saveValuationData(IntrinsicValue intrinsicValue, StockPrice stockPrice);
}
