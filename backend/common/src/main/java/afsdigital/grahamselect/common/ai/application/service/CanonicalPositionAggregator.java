package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioPosition;
import afsdigital.grahamselect.common.ai.domain.service.TickerAllowlistCatalog;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Shared deterministic canonicalization and aggregation helper used by both
 * {@link PiiSanitizer} and {@link PortfolioStateHashCalculator} to guarantee
 * identical manual labeling and position aggregation.
 */
final class CanonicalPositionAggregator {

    private static final Comparator<RawPortfolioPosition> MANUAL_POSITION_COMPARATOR =
            Comparator.comparing((RawPortfolioPosition p) -> normalizeRawKey(p.rawTicker()))
                    .thenComparing(p -> effectiveSubClass(p.subClass()).name())
                    .thenComparing(p -> p.qualityGrade() == null ? 0 : p.qualityGrade())
                    .thenComparing(RawPortfolioPosition::quarantineCorporateEvent)
                    .thenComparing(p -> nonNull(p.quantity()))
                    .thenComparing(p -> nonNull(p.weightPercent()))
                    .thenComparing(p -> nonNull(p.priceToEarnings()))
                    .thenComparing(p -> nonNull(p.priceToBook()))
                    .thenComparing(p -> nonNull(p.dividendYieldPercent()))
                    .thenComparing(p -> nonNull(p.grahamMarginPercent()));

    private CanonicalPositionAggregator() {
    }

    static List<AggregatedPosition> aggregate(
            List<RawPortfolioPosition> rawPositions,
            TickerAllowlistCatalog allowlistCatalog
    ) {
        if (rawPositions == null || rawPositions.isEmpty()) {
            return List.of();
        }

        List<CanonicalInput> resolved = new ArrayList<>(rawPositions.size());
        List<RawPortfolioPosition> manualPositions = new ArrayList<>();

        for (RawPortfolioPosition position : rawPositions) {
            Optional<String> canonical = allowlistCatalog.resolveCanonicalTicker(position.rawTicker());
            if (canonical.isPresent()) {
                resolved.add(new CanonicalInput(canonical.get(), position));
            } else {
                manualPositions.add(position);
            }
        }

        manualPositions.sort(MANUAL_POSITION_COMPARATOR);
        Map<MacroClass, Integer> manualCounters = new EnumMap<>(MacroClass.class);
        for (RawPortfolioPosition manual : manualPositions) {
            int nextIndex = manualCounters.getOrDefault(manual.macroClass(), 0) + 1;
            manualCounters.put(manual.macroClass(), nextIndex);
            String manualLabel = manual.macroClass().name() + "_MANUAL_" + nextIndex;
            resolved.add(new CanonicalInput(manualLabel, manual));
        }

        Map<GroupKey, MutableAggregate> grouped = new TreeMap<>();
        for (CanonicalInput input : resolved) {
            GroupKey key = new GroupKey(input.canonicalTicker(), input.position().macroClass());
            grouped.computeIfAbsent(key, k -> new MutableAggregate(k.canonicalTicker(), input.position()))
                    .merge(input.position());
        }

        List<AggregatedPosition> result = new ArrayList<>(grouped.size());
        for (MutableAggregate agg : grouped.values()) {
            result.add(agg.toImmutable());
        }
        return List.copyOf(result);
    }

    private static String normalizeRawKey(String rawTicker) {
        return rawTicker == null ? "" : rawTicker.trim().toUpperCase(Locale.ROOT);
    }

    private static CerradoSubClass effectiveSubClass(CerradoSubClass subClass) {
        return subClass == null ? CerradoSubClass.OUTROS_MANUAL : subClass;
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record GroupKey(String canonicalTicker, MacroClass macroClass) implements Comparable<GroupKey> {
        @Override
        public int compareTo(GroupKey other) {
            int tickerCmp = this.canonicalTicker.compareTo(other.canonicalTicker);
            return tickerCmp != 0 ? tickerCmp : this.macroClass.name().compareTo(other.macroClass.name());
        }
    }

    private record CanonicalInput(String canonicalTicker, RawPortfolioPosition position) {
    }

    static final class AggregatedPosition {
        final String canonicalTicker;
        final MacroClass macroClass;
        final CerradoSubClass subClass;
        final BigDecimal quantity;
        final BigDecimal weightPercent;
        final Integer qualityGrade;
        final boolean quarantineCorporateEvent;
        final BigDecimal priceToEarnings;
        final BigDecimal priceToBook;
        final BigDecimal dividendYieldPercent;
        final BigDecimal grahamMarginPercent;

        AggregatedPosition(
                String canonicalTicker,
                MacroClass macroClass,
                CerradoSubClass subClass,
                BigDecimal quantity,
                BigDecimal weightPercent,
                Integer qualityGrade,
                boolean quarantineCorporateEvent,
                BigDecimal priceToEarnings,
                BigDecimal priceToBook,
                BigDecimal dividendYieldPercent,
                BigDecimal grahamMarginPercent
        ) {
            this.canonicalTicker = canonicalTicker;
            this.macroClass = macroClass;
            this.subClass = subClass;
            this.quantity = quantity;
            this.weightPercent = weightPercent;
            this.qualityGrade = qualityGrade;
            this.quarantineCorporateEvent = quarantineCorporateEvent;
            this.priceToEarnings = priceToEarnings;
            this.priceToBook = priceToBook;
            this.dividendYieldPercent = dividendYieldPercent;
            this.grahamMarginPercent = grahamMarginPercent;
        }
    }

    private static final class MutableAggregate {
        private final String canonicalTicker;
        private final MacroClass macroClass;
        private CerradoSubClass subClass;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal weightPercent = BigDecimal.ZERO;
        private Integer qualityGrade;
        private boolean quarantineCorporateEvent;
        private BigDecimal priceToEarnings;
        private BigDecimal priceToBook;
        private BigDecimal dividendYieldPercent;
        private BigDecimal grahamMarginPercent;

        MutableAggregate(String canonicalTicker, RawPortfolioPosition first) {
            this.canonicalTicker = canonicalTicker;
            this.macroClass = first.macroClass();
            this.subClass = effectiveSubClass(first.subClass());
        }

        void merge(RawPortfolioPosition pos) {
            this.quantity = this.quantity.add(nonNull(pos.quantity()));
            this.weightPercent = this.weightPercent.add(nonNull(pos.weightPercent()));
            this.quarantineCorporateEvent = this.quarantineCorporateEvent || pos.quarantineCorporateEvent();

            if (pos.subClass() != null && pos.subClass() != CerradoSubClass.OUTROS_MANUAL) {
                if (this.subClass == CerradoSubClass.OUTROS_MANUAL
                        || pos.subClass().name().compareTo(this.subClass.name()) < 0) {
                    this.subClass = pos.subClass();
                }
            }
            if (pos.qualityGrade() != null) {
                this.qualityGrade = this.qualityGrade == null
                        ? pos.qualityGrade()
                        : Math.min(this.qualityGrade, pos.qualityGrade());
            }
            this.priceToEarnings = minNullable(this.priceToEarnings, pos.priceToEarnings());
            this.priceToBook = minNullable(this.priceToBook, pos.priceToBook());
            this.dividendYieldPercent = minNullable(this.dividendYieldPercent, pos.dividendYieldPercent());
            this.grahamMarginPercent = minNullable(this.grahamMarginPercent, pos.grahamMarginPercent());
        }

        private static BigDecimal minNullable(BigDecimal current, BigDecimal incoming) {
            if (incoming == null) {
                return current;
            }
            return current == null ? incoming : current.min(incoming);
        }

        AggregatedPosition toImmutable() {
            return new AggregatedPosition(
                    canonicalTicker,
                    macroClass,
                    subClass,
                    quantity,
                    weightPercent,
                    qualityGrade,
                    quarantineCorporateEvent,
                    priceToEarnings,
                    priceToBook,
                    dividendYieldPercent,
                    grahamMarginPercent
            );
        }
    }
}
