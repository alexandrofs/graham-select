package afsdigital.grahamselect.valuation.application.dto;

import afsdigital.grahamselect.common.domain.entities.ExcludedTicker;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import java.util.List;

public record GenerationResult(
    List<GrahamRecommendation> recommendations,
    List<ExcludedTicker> excludedTickers
) {}
