package afsdigital.grahamselect.common.portfolio.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.Instant;

public record CustodyPositionDTO(
        String ticker,
        
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal quantity,
        
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal averagePrice,
        
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal currentPrice,
        
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal marketValue,
        
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal gainLossPercentage,
        
        String priceSource, // "LIVE" ou "CACHE"
        
        Instant priceUpdatedAt
) {
}
