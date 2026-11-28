package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.application.service.CanonicalPositionAggregator.AggregatedPosition;
import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioSnapshot;
import afsdigital.grahamselect.common.ai.domain.service.TickerAllowlistCatalog;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * Computes a deterministic 64-character lowercase SHA-256 hash of the portfolio state
 * for cache-aside invalidation. Independent of position input order, BigDecimal scale,
 * and PII/monetary fields.
 */
public class PortfolioStateHashCalculator {

    private static final int QUANTITY_SCALE = 4;
    private static final int TARGET_SCALE = 4;

    private final TickerAllowlistCatalog allowlistCatalog;

    public PortfolioStateHashCalculator(TickerAllowlistCatalog allowlistCatalog) {
        this.allowlistCatalog = Objects.requireNonNull(allowlistCatalog, "allowlistCatalog must not be null");
    }

    public String calculate(RawPortfolioSnapshot rawSnapshot) {
        Objects.requireNonNull(rawSnapshot, "rawSnapshot must not be null");

        List<AggregatedPosition> aggregated = CanonicalPositionAggregator.aggregate(
                rawSnapshot.positions(),
                allowlistCatalog
        );

        StringBuilder canonical = new StringBuilder();
        canonical.append("POSITIONS:");
        for (AggregatedPosition pos : aggregated) {
            String qtyStr = pos.quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_EVEN).toPlainString();
            String gradeStr = pos.qualityGrade == null ? "NONE" : pos.qualityGrade.toString();
            canonical.append(pos.canonicalTicker)
                    .append(':')
                    .append(qtyStr)
                    .append(':')
                    .append(pos.quarantineCorporateEvent)
                    .append(':')
                    .append(gradeStr)
                    .append('|');
        }

        canonical.append("TARGETS:");
        for (MacroClass macroClass : MacroClass.values()) {
            BigDecimal target = rawSnapshot.targetAllocationsPercent()
                    .getOrDefault(macroClass, BigDecimal.ZERO)
                    .setScale(TARGET_SCALE, RoundingMode.HALF_EVEN);
            canonical.append(macroClass.name())
                    .append('=')
                    .append(target.toPlainString())
                    .append('|');
        }

        return sha256Hex(canonical.toString());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
