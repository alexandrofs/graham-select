package afsdigital.grahamselect.valuation.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
public class StockPrice {

    private LocalDate date;
    private String companyId;
    private BigDecimal price;

}
