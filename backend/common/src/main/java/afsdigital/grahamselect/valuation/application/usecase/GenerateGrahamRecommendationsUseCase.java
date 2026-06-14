package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.application.repository.PortfolioSnapshotPort;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GenerateGrahamRecommendationsUseCase {

    private final AllocationGoalPort allocationGoalPort;
    private final RankingRepository rankingRepository;
    private final PortfolioSnapshotPort portfolioSnapshotPort;
    private final GrahamRecommendationPort grahamRecommendationPort;

    public List<GrahamRecommendation> execute(String userId) {
        log.info("Generating Graham recommendations for user {}", userId);

        List<RankedCompany> rankedCompanies = rankingRepository.findTop20BestRanked();
        if (rankedCompanies == null || rankedCompanies.isEmpty()) {
            log.warn("No ranked companies found. Returning empty recommendations for user {}", userId);
            return List.of();
        }

        List<AllocationGoalDto> userGoals = allocationGoalPort.findByUserId(userId);
        boolean hasGoals = userGoals != null && !userGoals.isEmpty();
        Map<String, BigDecimal> currentAllocation = portfolioSnapshotPort.getCurrentAllocationByUserId(userId);

        Map<String, BigDecimal> goalsByTicker = Map.of();
        Map<String, BigDecimal> goalsByClass = Map.of();

        if (hasGoals) {
            goalsByTicker = userGoals.stream()
                    .filter(g -> g != null && g.targetKey() != null && g.targetPercentage() != null)
                    .filter(g -> "TICKER".equals(g.goalType()))
                    .collect(Collectors.toMap(
                            g -> g.targetKey().trim().toUpperCase(),
                            AllocationGoalDto::targetPercentage,
                            (a, b) -> a
                    ));

            goalsByClass = userGoals.stream()
                    .filter(g -> g != null && g.targetKey() != null && g.targetPercentage() != null)
                    .filter(g -> "ASSET_CLASS".equals(g.goalType()))
                    .collect(Collectors.toMap(
                            g -> g.targetKey().trim().toUpperCase(),
                            AllocationGoalDto::targetPercentage,
                            (a, b) -> a
                    ));
        }

        final Map<String, BigDecimal> finalGoalsByTicker = goalsByTicker;
        final Map<String, BigDecimal> finalGoalsByClass = goalsByClass;

        List<GrahamRecommendation> recommendations = rankedCompanies.stream()
                .filter(c -> c.getMarginOfSafety() != null && c.getMarginOfSafety().compareTo(BigDecimal.ZERO) > 0)
                .map(c -> {
                    String ticker = c.getSymbol().trim().toUpperCase();
                    String assetClass = inferAssetClass(ticker);

                    BigDecimal targetPct = BigDecimal.ZERO;
                    if (hasGoals) {
                        if (finalGoalsByTicker.containsKey(ticker)) {
                            targetPct = finalGoalsByTicker.get(ticker);
                        } else if (finalGoalsByClass.containsKey(assetClass)) {
                            targetPct = finalGoalsByClass.get(assetClass);
                        }
                    }

                    BigDecimal currentPct = currentAllocation.getOrDefault(c.getSymbol(), BigDecimal.ZERO);
                    BigDecimal allocationGap = targetPct.subtract(currentPct);
                    BigDecimal maxGap = allocationGap.max(BigDecimal.ZERO);

                    BigDecimal maxGapNormalized = maxGap.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);

                    // recommendationScore = marginOfSafety * 0.6 + max(allocationGap, 0) * 0.4
                    BigDecimal score = c.getMarginOfSafety().multiply(new BigDecimal("0.6"))
                            .add(maxGapNormalized.multiply(new BigDecimal("0.4")))
                            .setScale(4, RoundingMode.HALF_UP);

                    return GrahamRecommendation.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .ticker(c.getSymbol())
                            .currentPrice(c.getCurrentPrice())
                            .intrinsicValue(c.getIntrinsicValue())
                            .marginOfSafety(c.getMarginOfSafety())
                            .currentAllocationPct(currentPct)
                            .targetAllocationPct(targetPct)
                            .allocationGap(allocationGap)
                            .recommendationScore(score)
                            .generatedAt(LocalDate.now(ZoneOffset.UTC))
                            .epsUsed(c.getEps())
                            .bvpsUsed(c.getBvps())
                            .build();
                })
                .filter(r -> {
                    if (!hasGoals) {
                        return true;
                    }
                    boolean isCovered = finalGoalsByTicker.containsKey(r.getTicker().toUpperCase())
                            || finalGoalsByClass.containsKey(inferAssetClass(r.getTicker()));
                    return isCovered && r.getAllocationGap().compareTo(BigDecimal.ZERO) > 0;
                })
                .sorted(Comparator.comparing(GrahamRecommendation::getRecommendationScore).reversed())
                .toList();

        grahamRecommendationPort.saveRecommendations(userId, recommendations);
        log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
        return recommendations;
    }

    private String inferAssetClass(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return "OUTROS";
        }
        String t = ticker.trim().toUpperCase();

        if (t.endsWith("F") && t.length() > 4 && Character.isDigit(t.charAt(t.length() - 2))) {
            t = t.substring(0, t.length() - 1);
        }

        if (t.startsWith("TESOURO") || t.startsWith("CDB") || t.startsWith("LCI")
                || t.startsWith("LCA") || t.contains("DIRETO") || t.contains("DEBENTURE")) {
            return "RENDA_FIXA";
        }

        if (t.length() == 6 && t.endsWith("11")) {
            return "FIIS";
        }

        if (t.matches("^[A-Z]{4}34$")) {
            return "BDRS";
        }

        if (t.matches("^[A-Z]{4}[34568]$") || (t.length() == 5 && Character.isDigit(t.charAt(4)))) {
            return "ACOES";
        }

        return "OUTROS";
    }
}
