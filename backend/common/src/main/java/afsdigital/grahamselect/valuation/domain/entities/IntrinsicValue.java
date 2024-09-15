package afsdigital.grahamselect.valuation.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
public class IntrinsicValue {

    private LocalDate calculationDate;
    private String companyId;
    private BigDecimal value;

}
