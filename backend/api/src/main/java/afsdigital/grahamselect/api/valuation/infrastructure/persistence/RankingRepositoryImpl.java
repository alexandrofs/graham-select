package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.RankingJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RankingRepositoryImpl implements RankingRepository {

    private final RankingJpaRepository rankingJpaRepository;

    @Override
    public List<RankedCompany> findTop20BestRanked() {
        return rankingJpaRepository.findTop20BestRanked().stream()
                .map(entity -> RankedCompany.builder()
                        .symbol(entity.getSymbol())
                        .name(entity.getName())
                        .intrinsicValue(entity.getIntrinsicValue())
                        .currentPrice(entity.getCurrentPrice())
                        .marginOfSafety(entity.getMarginOfSafety())
                        .build())
                .toList();
    }
}
