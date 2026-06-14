package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntrinsicValueEntity {

    @Id
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "company_id", length = 36, columnDefinition = "VARCHAR(36)", nullable = false)
    private String companyId;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "intrinsic_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal intrinsicValue;

    @Column(name = "eps", precision = 18, scale = 4)
    private BigDecimal eps;

    @Column(name = "bvps", precision = 18, scale = 4)
    private BigDecimal bvps;

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_company"))
    private CompanyEntity company;

}
