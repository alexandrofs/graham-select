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
@Table(name = "stock_price")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceEntity {

    @Id
    private String id;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

}
