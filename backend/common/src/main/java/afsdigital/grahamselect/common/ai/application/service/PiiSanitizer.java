package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.application.service.CanonicalPositionAggregator.AggregatedPosition;
import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import afsdigital.grahamselect.common.ai.domain.service.TickerAllowlistCatalog;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LGPD privacy barrier that strips all PII, brokerage names, absolute BRL values,
 * and absolute share quantities, normalizing tickers against {@link TickerAllowlistCatalog}
 * and assigning deterministic anonymous {@code <MACRO_CLASS>_MANUAL_<N>} labels.
 */
public class PiiSanitizer {

    private static final int PERCENT_SCALE = 2;
    private static final int MULTIPLE_SCALE = 2;
    private static final int HHI_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TickerAllowlistCatalog allowlistCatalog;

    public PiiSanitizer(TickerAllowlistCatalog allowlistCatalog) {
        this.allowlistCatalog = Objects.requireNonNull(allowlistCatalog, "allowlistCatalog must not be null");
    }

    public SanitizedPortfolioSnapshot sanitize(RawPortfolioSnapshot rawSnapshot) {
        Objects.requireNonNull(rawSnapshot, "rawSnapshot must not be null");

        List<AggregatedPosition> aggregated = CanonicalPositionAggregator.aggregate(
                rawSnapshot.positions(),
                allowlistCatalog
        );

        List<SanitizedAssetPosition> sanitizedPositions = new ArrayList<>(aggregated.size());
        Map<MacroClass, BigDecimal> currentByMacroClass = new EnumMap<>(MacroClass.class);
        for (MacroClass macroClass : MacroClass.values()) {
            currentByMacroClass.put(macroClass, BigDecimal.ZERO);
        }

        for (AggregatedPosition pos : aggregated) {
            BigDecimal weight = scale(pos.weightPercent, PERCENT_SCALE);
            sanitizedPositions.add(new SanitizedAssetPosition(
                    pos.canonicalTicker,
                    pos.macroClass,
                    pos.subClass,
                    weight,
                    pos.qualityGrade,
                    pos.quarantineCorporateEvent,
                    scaleNullable(pos.priceToEarnings, MULTIPLE_SCALE),
                    scaleNullable(pos.priceToBook, MULTIPLE_SCALE),
                    scaleNullable(pos.dividendYieldPercent, PERCENT_SCALE),
                    scaleNullable(pos.grahamMarginPercent, PERCENT_SCALE)
            ));
            currentByMacroClass.merge(pos.macroClass, pos.weightPercent, BigDecimal::add);
        }

        List<MacroClassAllocationSnapshot> allocations = new ArrayList<>(MacroClass.values().length);
        for (MacroClass macroClass : MacroClass.values()) {
            BigDecimal current = scale(currentByMacroClass.get(macroClass), PERCENT_SCALE);
            BigDecimal target = scale(
                    rawSnapshot.targetAllocationsPercent().getOrDefault(macroClass, BigDecimal.ZERO),
                    PERCENT_SCALE
            );
            BigDecimal deviation = scale(current.subtract(target), PERCENT_SCALE);
            allocations.add(new MacroClassAllocationSnapshot(macroClass, current, target, deviation));
        }

        BigDecimal hhiIndex = resolveHhiIndex(rawSnapshot.hhiIndex(), aggregated);
        return new SanitizedPortfolioSnapshot(sanitizedPositions, allocations, hhiIndex);
    }

    private static BigDecimal resolveHhiIndex(
            BigDecimal rawHhiIndex,
            List<AggregatedPosition> aggregatedPositions
    ) {
        if (rawHhiIndex != null) {
            return scale(rawHhiIndex, HHI_SCALE);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (AggregatedPosition position : aggregatedPositions) {
            BigDecimal share = position.weightPercent.divide(ONE_HUNDRED, 8, RoundingMode.HALF_EVEN);
            sum = sum.add(share.multiply(share));
        }
        return scale(sum, HHI_SCALE);
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(scale, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal scaleNullable(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_EVEN);
    }
}
