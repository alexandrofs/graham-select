package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import java.util.List;

public interface RankingRepository {

    List<RankedCompany> findTop20BestRanked();

}
