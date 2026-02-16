package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetRankedCompaniesUseCase {

    private final RankingRepository rankingRepository;

    public List<RankedCompany> execute() {
        return rankingRepository.findTop20BestRanked();
    }
}
