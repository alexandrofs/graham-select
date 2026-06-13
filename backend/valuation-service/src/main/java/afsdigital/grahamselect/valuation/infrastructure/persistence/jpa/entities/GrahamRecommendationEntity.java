package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "graham_recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrahamRecommendationEntity {

    @Id
    @Column(name = "id", length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "current_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "intrinsic_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal intrinsicValue;

    @Column(name = "margin_of_safety", nullable = false, precision = 8, scale = 4)
    private BigDecimal marginOfSafety;

    @Column(name = "current_allocation_pct", nullable = false, precision = 8, scale = 4)
    private BigDecimal currentAllocationPct;

    @Column(name = "target_allocation_pct", nullable = false, precision = 8, scale = 4)
    private BigDecimal targetAllocationPct;

    @Column(name = "allocation_gap", nullable = false, precision = 8, scale = 4)
    private BigDecimal allocationGap;

    @Column(name = "recommendation_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal recommendationScore;

    @Column(name = "generated_at", nullable = false)
    private LocalDate generatedAt;

    @Column(name = "eps_used", precision = 18, scale = 4)
    private BigDecimal epsUsed;

    @Column(name = "bvps_used", precision = 18, scale = 4)
    private BigDecimal bvpsUsed;

}
