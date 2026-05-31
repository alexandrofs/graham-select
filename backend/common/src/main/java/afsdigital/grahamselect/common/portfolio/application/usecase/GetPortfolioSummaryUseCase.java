package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.domain.repository.StockPricePort;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GetPortfolioSummaryUseCase {

    private final TradePort tradePort;
    private final CompanyRepository companyRepository;
    private final StockPricePort stockPricePort;

    public PortfolioSummaryDTO execute(String userId) {
        List<Trade> trades = tradePort.findAllByUserId(userId);

        Map<String, BigDecimal> positions = new HashMap<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal accumulatedDividends = BigDecimal.ZERO;

        for (Trade trade : trades) {
            String ticker = trade.getTicker();
            BigDecimal quantity = trade.getQuantity();
            BigDecimal price = trade.getPrice();

            if (TradeSide.COMPRA.name().equals(trade.getSide())) {
                positions.merge(ticker, quantity, BigDecimal::add);
                totalInvested = totalInvested.add(quantity.multiply(price));
            } else if (TradeSide.VENDA.name().equals(trade.getSide())) {
                positions.merge(ticker, quantity.negate(), BigDecimal::add);
                totalInvested = totalInvested.subtract(quantity.multiply(price));
            } else if (TradeSide.DIVIDENDO.name().equals(trade.getSide())) {
                accumulatedDividends = accumulatedDividends.add(quantity.multiply(price));
            }
        }

        // Filter only active positions to get tickers
        List<String> activeTickers = positions.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(Map.Entry::getKey)
                .toList();

        // Map Tickers to Companies (and collect company IDs)
        Map<String, Company> tickerToCompany = activeTickers.stream()
                .map(companyRepository::findByTicker)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Company::getTicker, c -> c));

        List<String> companyIds = tickerToCompany.values().stream()
                .map(Company::getId)
                .toList();

        // Batch fetch prices
        Map<String, StockPrice> latestPrices = stockPricePort.findLatestByCompanyIds(companyIds);

        BigDecimal totalEquity = BigDecimal.ZERO;
        for (String ticker : activeTickers) {
            BigDecimal qty = positions.get(ticker);
            Company company = tickerToCompany.get(ticker);
            
            if (company != null) {
                StockPrice sp = latestPrices.get(company.getId());
                if (sp != null) {
                    totalEquity = totalEquity.add(qty.multiply(sp.getPrice()));
                } else {
                    log.warn("Cotação não encontrada para o ticker: {} (Company ID: {}). O patrimônio total pode estar incompleto.", ticker, company.getId());
                }
            } else {
                log.warn("Empresa não encontrada no cadastro para o ticker: {}. Pulando cálculo de patrimônio para este ativo.", ticker);
            }
        }

        BigDecimal grossYieldPercentage = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            grossYieldPercentage = totalEquity.add(accumulatedDividends)
                    .subtract(totalInvested)
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal monthlyProjection = accumulatedDividends; // Default for MVP
        
        return new PortfolioSummaryDTO(
                totalEquity,
                grossYieldPercentage,
                accumulatedDividends,
                monthlyProjection
        );
    }
}
