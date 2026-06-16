package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedCompanyEntity {

    @Id
    private String symbol;
    private String name;
    private BigDecimal intrinsicValue;
    private BigDecimal currentPrice;
    private BigDecimal marginOfSafety;
    private LocalDate intrinsicValueUpdatedAt;

}
