package afsdigital.grahamselect.common.ai.domain.model;

import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Unsanitized portfolio snapshot containing PII and absolute BRL net worth fields for input only.
 * All PII and monetary values must be discarded before creating {@link SanitizedPortfolioSnapshot}.
 */
public record RawPortfolioSnapshot(
        Long userId,
        String ownerName,
        String cpf,
        String email,
        String phone,
        BigDecimal totalNetWorthBrl,
        List<RawPortfolioPosition> positions,
        Map<MacroClass, BigDecimal> targetAllocationsPercent,
        BigDecimal hhiIndex
) {
    public RawPortfolioSnapshot {
        positions = positions == null ? List.of() : List.copyOf(positions);
        targetAllocationsPercent = targetAllocationsPercent == null
                ? Map.of()
                : Map.copyOf(targetAllocationsPercent);
        for (BigDecimal target : targetAllocationsPercent.values()) {
            if (target == null || target.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("targetAllocationsPercent values must be >= 0");
            }
        }
        if (hhiIndex != null && hhiIndex.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("hhiIndex must be >= 0");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long userId;
        private String ownerName;
        private String cpf;
        private String email;
        private String phone;
        private BigDecimal totalNetWorthBrl;
        private List<RawPortfolioPosition> positions = List.of();
        private Map<MacroClass, BigDecimal> targetAllocationsPercent = Map.of();
        private BigDecimal hhiIndex;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public Builder cpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder totalNetWorthBrl(BigDecimal totalNetWorthBrl) {
            this.totalNetWorthBrl = totalNetWorthBrl;
            return this;
        }

        public Builder positions(List<RawPortfolioPosition> positions) {
            this.positions = positions;
            return this;
        }

        public Builder targetAllocationsPercent(Map<MacroClass, BigDecimal> targetAllocationsPercent) {
            this.targetAllocationsPercent = targetAllocationsPercent;
            return this;
        }

        public Builder hhiIndex(BigDecimal hhiIndex) {
            this.hhiIndex = hhiIndex;
            return this;
        }

        public RawPortfolioSnapshot build() {
            return new RawPortfolioSnapshot(
                    userId,
                    ownerName,
                    cpf,
                    email,
                    phone,
                    totalNetWorthBrl,
                    positions,
                    targetAllocationsPercent,
                    hhiIndex
            );
        }
    }
}
