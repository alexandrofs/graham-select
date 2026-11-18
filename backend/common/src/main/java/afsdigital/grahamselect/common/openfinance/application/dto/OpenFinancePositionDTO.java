package afsdigital.grahamselect.common.openfinance.application.dto;

import java.math.BigDecimal;

public record OpenFinancePositionDTO(
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePrice,
        String broker
) {}
