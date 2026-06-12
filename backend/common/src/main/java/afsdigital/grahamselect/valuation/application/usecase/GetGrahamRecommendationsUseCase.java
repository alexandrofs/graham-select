package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.dto.GrahamRecommendationDto;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetGrahamRecommendationsUseCase {

    private final GrahamRecommendationPort grahamRecommendationPort;

    public List<GrahamRecommendationDto> execute(String userId) {
        log.info("Fetching Graham recommendations for user {}", userId);
        List<GrahamRecommendation> recommendations = grahamRecommendationPort.findByUserId(userId);
        if (recommendations == null) {
            return List.of();
        }
        return recommendations.stream()
                .map(r -> new GrahamRecommendationDto(
                        r.getTicker(),
                        r.getCurrentPrice(),
                        r.getIntrinsicValue(),
                        r.getMarginOfSafety(),
                        r.getCurrentAllocationPct(),
                        r.getTargetAllocationPct(),
                        r.getAllocationGap(),
                        r.getRecommendationScore()
                ))
                .toList();
    }
}
