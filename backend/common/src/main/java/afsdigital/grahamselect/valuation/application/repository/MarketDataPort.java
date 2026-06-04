package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.application.dto.MarketDataResult;
import java.util.List;

public interface MarketDataPort {
    List<MarketDataResult> fetchMarketData(List<String> tickers);
}
