package afsdigital.grahamselect.valuation.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
public class RankedCompany {

    private String symbol;
    private String name;
    private BigDecimal intrinsicValue;
    private BigDecimal currentPrice;
    private BigDecimal marginOfSafety;
    private LocalDate intrinsicValueUpdatedAt;

}
