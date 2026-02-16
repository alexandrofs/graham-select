package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RankCompaniesService {

    private final RankingRepository rankingRepository;

    public List<IntrinsicValue> rankCompanies() {
        log.info("Fetching top 20 companies ranked by intrinsic value / price ratio");
        return rankingRepository.findTop20BestRanked();
    }

}
