package afsdigital.grahamselect.common.portfolio.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Trade {
    private UUID id;
    private String userId;
    private String ticker;
    private String side; // COMPRA, VENDA
    private LocalDate tradeDate;
    private BigDecimal quantity;
    private BigDecimal price;
    private String broker;
    private LocalDateTime createdAt;
}
