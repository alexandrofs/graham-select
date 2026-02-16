package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import java.util.List;

public interface RankingRepository {

    List<IntrinsicValue> findTop20BestRanked();

}
