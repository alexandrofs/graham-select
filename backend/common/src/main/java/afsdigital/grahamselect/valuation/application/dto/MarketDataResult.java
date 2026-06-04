package afsdigital.grahamselect.valuation.application.dto;

import java.math.BigDecimal;

public record MarketDataResult(
        String ticker,
        BigDecimal price,
        Double dividendYield,
        Double priceEarnings,
        Double priceToBook,
        Double earningsPerShare,
        Double bookValuePerShare
) {}
