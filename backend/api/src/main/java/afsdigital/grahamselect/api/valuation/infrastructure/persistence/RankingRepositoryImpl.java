package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.RankingJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RankingRepositoryImpl implements RankingRepository {

    private final RankingJpaRepository rankingJpaRepository;

    @Override
    public List<RankedCompany> findTop20BestRanked() {
        log.info("Querying top 20 ranked companies from database");
        return rankingJpaRepository.findTop20BestRanked().stream()
                .map(entity -> RankedCompany.builder()
                        .symbol(entity.getSymbol())
                        .name(entity.getName())
                        .intrinsicValue(entity.getIntrinsicValue())
                        .currentPrice(entity.getCurrentPrice())
                        .marginOfSafety(entity.getMarginOfSafety())
                        .intrinsicValueUpdatedAt(entity.getIntrinsicValueUpdatedAt())
                        .eps(entity.getEps())
                        .bvps(entity.getBvps())
                        .build())
                .toList();
    }
}
