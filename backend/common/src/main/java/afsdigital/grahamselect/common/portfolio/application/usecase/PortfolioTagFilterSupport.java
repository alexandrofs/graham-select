package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Shared pure-Java helper for validating portfolio tag ownership, matching tickers
 * (including B3 fractional suffix equivalence such as PETR4 / PETR4F), and
 * identifying globally untagged active portfolio tickers.
 */
@Slf4j
final class PortfolioTagFilterSupport {

    private PortfolioTagFilterSupport() {
    }

    static Set<String> resolveAllowedTickers(
            String userId,
            UUID tagId,
            PortfolioTagPort portfolioTagPort,
            AssetTagPort assetTagPort
    ) {
        if (tagId == null) {
            return null;
        }
        if (portfolioTagPort == null || assetTagPort == null) {
            IllegalStateException ex = new IllegalStateException("Portas de tag não configuradas para filtro de carteira.");
            log.error("Falha ao resolver filtro por tag: portas não configuradas. userId={}, tagId={}", sanitize(userId), tagId, ex);
            throw ex;
        }
        if (portfolioTagPort.findByIdAndUserId(tagId, userId).isEmpty()) {
            TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
            log.error("Acesso negado ou tag inexistente para filtro de carteira. userId={}, tagId={}", sanitize(userId), tagId, ex);
            throw ex;
        }

        List<AssetTag> taggedAssets = assetTagPort.findAllByUserIdAndTagId(userId, tagId);
        Set<String> allowed = new HashSet<>();
        if (taggedAssets != null) {
            for (AssetTag assetTag : taggedAssets) {
                addNormalizedVariants(allowed, assetTag != null ? assetTag.getTicker() : null);
            }
        }
        return allowed;
    }

    static List<Trade> filterTrades(List<Trade> trades, Set<String> allowedTickers) {
        if (trades == null || trades.isEmpty()) {
            return List.of();
        }
        if (allowedTickers == null) {
            return trades.stream().filter(Objects::nonNull).toList();
        }
        if (allowedTickers.isEmpty()) {
            return List.of();
        }
        return trades.stream()
                .filter(Objects::nonNull)
                .filter(trade -> matchesTicker(trade.getTicker(), allowedTickers))
                .toList();
    }

    static List<String> findUntaggedTickers(
            Collection<String> activeGlobalTickers,
            String userId,
            AssetTagPort assetTagPort
    ) {
        if (assetTagPort == null || activeGlobalTickers == null || activeGlobalTickers.isEmpty()) {
            return List.of();
        }
        List<AssetTag> allUserTags = assetTagPort.findAllByUserId(userId);
        Set<String> taggedSet = new HashSet<>();
        if (allUserTags != null) {
            for (AssetTag assetTag : allUserTags) {
                addNormalizedVariants(taggedSet, assetTag != null ? assetTag.getTicker() : null);
            }
        }
        return activeGlobalTickers.stream()
                .filter(Objects::nonNull)
                .map(PortfolioTagFilterSupport::normalizeTicker)
                .filter(ticker -> !ticker.isEmpty())
                .filter(ticker -> !matchesTicker(ticker, taggedSet))
                .distinct()
                .sorted()
                .toList();
    }

    static String stripFractionalSuffix(String ticker) {
        String normalized = normalizeTicker(ticker);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.endsWith("F") && normalized.length() > 4 && Character.isDigit(normalized.charAt(normalized.length() - 2))) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void addNormalizedVariants(Set<String> target, String ticker) {
        String exact = normalizeTicker(ticker);
        if (!exact.isEmpty()) {
            target.add(exact);
            String canonical = stripFractionalSuffix(exact);
            if (!canonical.isEmpty()) {
                target.add(canonical);
            }
        }
    }

    private static boolean matchesTicker(String ticker, Set<String> allowedSet) {
        String exact = normalizeTicker(ticker);
        if (exact.isEmpty()) {
            return false;
        }
        return allowedSet.contains(exact) || allowedSet.contains(stripFractionalSuffix(exact));
    }

    private static String normalizeTicker(String ticker) {
        return ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT);
    }

    private static String sanitize(String value) {
        return value == null ? "null" : value.replaceAll("[\\r\\n]", "_");
    }
}
