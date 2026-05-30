package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.application.repository.StockPricePort;
import afsdigital.grahamselect.valuation.domain.entities.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GetCustodyPositionsUseCase {

    private final TradePort tradePort;
    private final CompanyRepository companyRepository;
    private final StockPricePort stockPricePort;

    public List<CustodyPositionDTO> execute(String userId) {
        // Ordenar trades por data cronologicamente de forma robusta
        List<Trade> trades = tradePort.findAllByUserId(userId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Trade::getTradeDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // 1. Agrupar trades por Ticker (somar COMPRA, subtrair VENDA, excluir DIVIDENDO)
        // Também calcular Preço Médio Ponderado das Compras
        Map<String, BigDecimal> quantities = new HashMap<>();
        Map<String, BigDecimal> totalPurchaseAmount = new HashMap<>();
        Map<String, BigDecimal> totalPurchaseQty = new HashMap<>();

        for (Trade trade : trades) {
            String ticker = trade.getTicker();
            BigDecimal quantity = trade.getQuantity();
            BigDecimal price = trade.getPrice();

            if (TradeSide.COMPRA.name().equals(trade.getSide())) {
                quantities.merge(ticker, quantity, BigDecimal::add);
                totalPurchaseAmount.merge(ticker, quantity.multiply(price), BigDecimal::add);
                totalPurchaseQty.merge(ticker, quantity, BigDecimal::add);
            } else if (TradeSide.VENDA.name().equals(trade.getSide())) {
                BigDecimal currentQty = quantities.getOrDefault(ticker, BigDecimal.ZERO);
                BigDecimal newQty = currentQty.subtract(quantity);
                quantities.put(ticker, newQty);

                // Se liquidar a posicao (quantidade zerada ou menor), reseta os acumulados daquele ticker
                if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                    totalPurchaseAmount.put(ticker, BigDecimal.ZERO);
                    totalPurchaseQty.put(ticker, BigDecimal.ZERO);
                    quantities.put(ticker, BigDecimal.ZERO);
                }
            }
            // Trades do tipo DIVIDENDO devem ser excluídos
        }

        // Filtrar apenas tickers com quantity > 0
        List<String> activeTickers = quantities.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(Map.Entry::getKey)
                .toList();

        if (activeTickers.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Map Tickers to Companies
        Map<String, Company> tickerToCompany = activeTickers.stream()
                .map(companyRepository::findByTicker)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Company::getTicker, c -> c));

        List<String> companyIds = tickerToCompany.values().stream()
                .map(Company::getId)
                .toList();

        // 3. Batch fetch prices
        Map<String, StockPrice> latestPrices = stockPricePort.findLatestByCompanyIds(companyIds);

        List<CustodyPositionDTO> result = new ArrayList<>();

        for (String ticker : activeTickers) {
            BigDecimal quantity = quantities.get(ticker);
            
            // Preço Médio Ponderado: totalPurchaseAmount / totalPurchaseQty
            BigDecimal averagePrice = BigDecimal.ZERO;
            BigDecimal purQty = totalPurchaseQty.get(ticker);
            if (purQty != null && purQty.compareTo(BigDecimal.ZERO) > 0) {
                averagePrice = totalPurchaseAmount.get(ticker).divide(purQty, 4, RoundingMode.HALF_UP);
            }

            Company company = tickerToCompany.get(ticker);
            BigDecimal currentPrice = averagePrice; // Fallback se não encontrar cotação
            String priceSource = "CACHE";
            Instant priceUpdatedAt = null;

            if (company != null) {
                StockPrice sp = latestPrices.get(company.getId());
                if (sp != null) {
                    currentPrice = sp.getPrice();
                    if (sp.getDate() != null) {
                        priceUpdatedAt = sp.getDate().atStartOfDay(ZoneOffset.UTC).toInstant();
                        
                        LocalDate priceDate = sp.getDate();
                        LocalDate today = LocalDate.now(ZoneOffset.UTC);
                        if (!priceDate.isBefore(today)) {
                            priceSource = "LIVE";
                        } else {
                            priceSource = "CACHE";
                        }
                    }
                } else {
                    log.warn("Cotação não encontrada para o ticker: {} (Company ID: {}). Usando preço médio como fallback.", ticker, company.getId());
                }
            } else {
                log.warn("Empresa não encontrada no cadastro para o ticker: {}. Usando preço médio como fallback.", ticker);
            }

            // Calcular marketValue = quantity * currentPrice
            BigDecimal marketValue = quantity.multiply(currentPrice);

            // Calcular gainLossPercentage = ((currentPrice - averagePrice) / averagePrice) * 100
            BigDecimal gainLossPercentage = BigDecimal.ZERO;
            if (averagePrice.compareTo(BigDecimal.ZERO) > 0) {
                gainLossPercentage = currentPrice.subtract(averagePrice)
                        .divide(averagePrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            result.add(new CustodyPositionDTO(
                    ticker,
                    quantity.setScale(2, RoundingMode.HALF_UP),
                    averagePrice.setScale(2, RoundingMode.HALF_UP),
                    currentPrice.setScale(2, RoundingMode.HALF_UP),
                    marketValue.setScale(2, RoundingMode.HALF_UP),
                    gainLossPercentage.setScale(2, RoundingMode.HALF_UP),
                    priceSource,
                    priceUpdatedAt
            ));
        }

        return result;
    }
}
