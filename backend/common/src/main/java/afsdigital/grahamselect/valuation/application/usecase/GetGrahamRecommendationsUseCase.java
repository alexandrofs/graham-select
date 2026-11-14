package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.valuation.application.dto.GrahamRecommendationDto;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class GetGrahamRecommendationsUseCase {

    private final GrahamRecommendationPort grahamRecommendationPort;
    private final PortfolioTagPort portfolioTagPort;

    public GetGrahamRecommendationsUseCase(GrahamRecommendationPort grahamRecommendationPort) {
        this(grahamRecommendationPort, null);
    }

    public GetGrahamRecommendationsUseCase(
            GrahamRecommendationPort grahamRecommendationPort,
            PortfolioTagPort portfolioTagPort
    ) {
        this.grahamRecommendationPort = grahamRecommendationPort;
        this.portfolioTagPort = portfolioTagPort;
    }

    public List<GrahamRecommendationDto> execute(String userId) {
        return execute(userId, null);
    }

    public List<GrahamRecommendationDto> execute(String userId, UUID tagId) {
        log.info("Fetching Graham recommendations for user {}, tagId={}", sanitize(userId), tagId);
        List<GrahamRecommendation> recommendations;
        if (tagId == null) {
            recommendations = grahamRecommendationPort.findByUserId(userId);
        } else {
            validateTagOwnership(userId, tagId);
            recommendations = grahamRecommendationPort.findByUserId(userId, tagId);
        }
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
                        r.getRecommendationScore(),
                        r.getEpsUsed(),
                        r.getBvpsUsed()
                ))
                .toList();
    }

    private void validateTagOwnership(String userId, UUID tagId) {
        if (portfolioTagPort == null) {
            IllegalStateException ex = new IllegalStateException("PortfolioTagPort não configurado para recomendações.");
            log.error("Falha ao validar propriedade da tag {} para recomendações do usuário {}", tagId, sanitize(userId), ex);
            throw ex;
        }
        if (portfolioTagPort.findByIdAndUserId(tagId, userId).isEmpty()) {
            TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
            log.error("Tag não encontrada ou acesso negado em recomendações. userId={}, tagId={}", sanitize(userId), tagId, ex);
            throw ex;
        }
    }

    private static String sanitize(String value) {
        return value == null ? "null" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
