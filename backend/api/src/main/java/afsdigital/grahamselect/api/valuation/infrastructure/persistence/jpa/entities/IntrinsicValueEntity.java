package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities;

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
@Table(name = "company_intrinsic_value")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrinsicValueEntity {

    @Id
    private String id;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(name = "intrinsic_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal intrinsicValue;

    @Column(name = "eps", precision = 18, scale = 4)
    private BigDecimal eps;

    @Column(name = "bvps", precision = 18, scale = 4)
    private BigDecimal bvps;

}
