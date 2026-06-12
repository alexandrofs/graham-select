package afsdigital.grahamselect.valuation.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrahamRecommendation {

    private UUID id;
    private String userId;
    private String ticker;
    private BigDecimal currentPrice;
    private BigDecimal intrinsicValue;
    private BigDecimal marginOfSafety;
    private BigDecimal currentAllocationPct;
    private BigDecimal targetAllocationPct;
    private BigDecimal allocationGap;
    private BigDecimal recommendationScore;
    private LocalDate generatedAt;

}
