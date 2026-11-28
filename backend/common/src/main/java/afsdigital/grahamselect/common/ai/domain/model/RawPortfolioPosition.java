package afsdigital.grahamselect.common.ai.domain.model;

import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Unsanitized portfolio position input containing raw ticker/description, absolute quantity
 * (used only for deterministic state hashing) and sensitive monetary/brokerage fields that
 * must be discarded by {@code PiiSanitizer}.
 */
public record RawPortfolioPosition(
        String rawTicker,
        MacroClass macroClass,
        CerradoSubClass subClass,
        BigDecimal quantity,
        BigDecimal weightPercent,
        BigDecimal marketValueBrl,
        BigDecimal averagePriceBrl,
        String brokerName,
        String accountLabel,
        Integer qualityGrade,
        boolean quarantineCorporateEvent,
        BigDecimal priceToEarnings,
        BigDecimal priceToBook,
        BigDecimal dividendYieldPercent,
        BigDecimal grahamMarginPercent
) {
    public RawPortfolioPosition {
        Objects.requireNonNull(macroClass, "macroClass must not be null");
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (weightPercent != null
                && (weightPercent.compareTo(BigDecimal.ZERO) < 0
                || weightPercent.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("weightPercent must be between 0 and 100");
        }
        if (qualityGrade != null && (qualityGrade < 1 || qualityGrade > 10)) {
            throw new IllegalArgumentException("qualityGrade must be between 1 and 10");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String rawTicker;
        private MacroClass macroClass;
        private CerradoSubClass subClass = CerradoSubClass.OUTROS_MANUAL;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal weightPercent = BigDecimal.ZERO;
        private BigDecimal marketValueBrl;
        private BigDecimal averagePriceBrl;
        private String brokerName;
        private String accountLabel;
        private Integer qualityGrade;
        private boolean quarantineCorporateEvent;
        private BigDecimal priceToEarnings;
        private BigDecimal priceToBook;
        private BigDecimal dividendYieldPercent;
        private BigDecimal grahamMarginPercent;

        public Builder rawTicker(String rawTicker) {
            this.rawTicker = rawTicker;
            return this;
        }

        public Builder macroClass(MacroClass macroClass) {
            this.macroClass = macroClass;
            return this;
        }

        public Builder subClass(CerradoSubClass subClass) {
            this.subClass = subClass;
            return this;
        }

        public Builder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder weightPercent(BigDecimal weightPercent) {
            this.weightPercent = weightPercent;
            return this;
        }

        public Builder marketValueBrl(BigDecimal marketValueBrl) {
            this.marketValueBrl = marketValueBrl;
            return this;
        }

        public Builder averagePriceBrl(BigDecimal averagePriceBrl) {
            this.averagePriceBrl = averagePriceBrl;
            return this;
        }

        public Builder brokerName(String brokerName) {
            this.brokerName = brokerName;
            return this;
        }

        public Builder accountLabel(String accountLabel) {
            this.accountLabel = accountLabel;
            return this;
        }

        public Builder qualityGrade(Integer qualityGrade) {
            this.qualityGrade = qualityGrade;
            return this;
        }

        public Builder quarantineCorporateEvent(boolean quarantineCorporateEvent) {
            this.quarantineCorporateEvent = quarantineCorporateEvent;
            return this;
        }

        public Builder priceToEarnings(BigDecimal priceToEarnings) {
            this.priceToEarnings = priceToEarnings;
            return this;
        }

        public Builder priceToBook(BigDecimal priceToBook) {
            this.priceToBook = priceToBook;
            return this;
        }

        public Builder dividendYieldPercent(BigDecimal dividendYieldPercent) {
            this.dividendYieldPercent = dividendYieldPercent;
            return this;
        }

        public Builder grahamMarginPercent(BigDecimal grahamMarginPercent) {
            this.grahamMarginPercent = grahamMarginPercent;
            return this;
        }

        public RawPortfolioPosition build() {
            return new RawPortfolioPosition(
                    rawTicker,
                    macroClass,
                    subClass,
                    quantity,
                    weightPercent,
                    marketValueBrl,
                    averagePriceBrl,
                    brokerName,
                    accountLabel,
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
