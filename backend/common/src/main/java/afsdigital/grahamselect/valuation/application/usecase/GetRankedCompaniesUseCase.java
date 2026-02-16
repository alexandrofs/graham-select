package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetRankedCompaniesUseCase {

    private final RankingRepository rankingRepository;

    public List<RankedCompany> execute() {
        log.info("Executing use case to fetch top 20 ranked companies");
        return rankingRepository.findTop20BestRanked();
    }
}
