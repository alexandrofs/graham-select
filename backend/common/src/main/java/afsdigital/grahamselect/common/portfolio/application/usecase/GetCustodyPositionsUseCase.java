package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.domain.repository.StockPricePort;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GetCustodyPositionsUseCase {

    public record CustodyPositionsResult(
            List<CustodyPositionDTO> positions,
            List<String> untaggedTickers
    ) {
    }

    private final TradePort tradePort;
    private final CompanyRepository companyRepository;
    private final StockPricePort stockPricePort;
    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public GetCustodyPositionsUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository,
            StockPricePort stockPricePort
    ) {
        this(tradePort, companyRepository, stockPricePort, null, null);
    }

    public GetCustodyPositionsUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository,
            StockPricePort stockPricePort,
            PortfolioTagPort portfolioTagPort,
            AssetTagPort assetTagPort
    ) {
        this.tradePort = tradePort;
        this.companyRepository = companyRepository;
        this.stockPricePort = stockPricePort;
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public List<CustodyPositionDTO> execute(String userId) {
        return execute(userId, null);
    }

    public List<CustodyPositionDTO> execute(String userId, UUID tagId) {
        return executeWithMetadata(userId, tagId).positions();
    }

    public CustodyPositionsResult executeWithMetadata(String userId, UUID tagId) {
        Set<String> allowedTickers = PortfolioTagFilterSupport.resolveAllowedTickers(
                userId, tagId, portfolioTagPort, assetTagPort
        );

        // Ordenar trades globais por data cronologicamente de forma determinística
        List<Trade> allTrades = tradePort.findAllByUserId(userId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(Trade::getTradeDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Trade::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        List<String> globalActiveTickers = computeActiveTickers(allTrades);
        List<String> untaggedTickers = PortfolioTagFilterSupport.findUntaggedTickers(
                globalActiveTickers, userId, assetTagPort
        );

        List<Trade> trades = PortfolioTagFilterSupport.filterTrades(allTrades, allowedTickers);

        // 1. Agrupar trades por Ticker (somar COMPRA, subtrair VENDA, excluir DIVIDENDO)
        // Também calcular Preço Médio Ponderado das Compras
        Map<String, BigDecimal> quantities = new HashMap<>();
        Map<String, BigDecimal> totalPurchaseAmount = new HashMap<>();
        Map<String, BigDecimal> totalPurchaseQty = new HashMap<>();
        Map<String, String> tradeAssetClasses = new HashMap<>();

        for (Trade trade : trades) {
            String ticker = trade.getTicker();
            BigDecimal quantity = trade.getQuantity();
            BigDecimal price = trade.getPrice();

            if (trade.getAssetClass() != null && !trade.getAssetClass().isBlank()) {
                tradeAssetClasses.put(ticker, trade.getAssetClass().trim());
            }

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
            return new CustodyPositionsResult(Collections.emptyList(), untaggedTickers);
        }

        // 2. Map Tickers to Companies (with fractional suffix fallback, e.g. BOVA11F -> BOVA11)
        Map<String, Company> tickerToCompany = new HashMap<>();
        for (String activeTicker : activeTickers) {
            Company company = companyRepository.findByTicker(activeTicker);
            if (company == null) {
                String baseTicker = stripFractionalSuffix(activeTicker);
                if (!baseTicker.equalsIgnoreCase(activeTicker)) {
                    company = companyRepository.findByTicker(baseTicker);
                }
            }
            if (company != null) {
                tickerToCompany.putIfAbsent(activeTicker, company);
            }
        }

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

            String tradeAssetClass = tradeAssetClasses.get(ticker);
            String assetClass;
            if (tradeAssetClass != null && !tradeAssetClass.isBlank()) {
                assetClass = tradeAssetClass;
            } else if (company != null && company.getAssetClass() != null && !company.getAssetClass().isBlank()) {
                assetClass = company.getAssetClass();
            } else {
                assetClass = inferAssetClass(ticker);
            }

            result.add(new CustodyPositionDTO(
                    ticker,
                    quantity.setScale(2, RoundingMode.HALF_UP),
                    averagePrice.setScale(2, RoundingMode.HALF_UP),
                    currentPrice.setScale(2, RoundingMode.HALF_UP),
                    marketValue.setScale(2, RoundingMode.HALF_UP),
                    gainLossPercentage.setScale(2, RoundingMode.HALF_UP),
                    priceSource,
                    priceUpdatedAt,
                    assetClass
            ));
        }

        return new CustodyPositionsResult(result, untaggedTickers);
    }

    private List<String> computeActiveTickers(List<Trade> orderedTrades) {
        if (orderedTrades.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, BigDecimal> quantities = new HashMap<>();
        for (Trade trade : orderedTrades) {
            String ticker = trade.getTicker();
            BigDecimal quantity = trade.getQuantity();
            if (TradeSide.COMPRA.name().equals(trade.getSide())) {
                quantities.merge(ticker, quantity, BigDecimal::add);
            } else if (TradeSide.VENDA.name().equals(trade.getSide())) {
                BigDecimal newQty = quantities.getOrDefault(ticker, BigDecimal.ZERO).subtract(quantity);
                quantities.put(ticker, newQty.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : newQty);
            }
        }
        return quantities.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String stripFractionalSuffix(String ticker) {
        return PortfolioTagFilterSupport.stripFractionalSuffix(ticker);
    }

    private static final Set<String> CRYPTO_TICKERS = Set.of(
            "BTC", "ETH", "SOL", "USDT", "USDC", "BNB", "XRP", "ADA", "DOGE",
            "MATIC", "DOT", "AVAX", "LINK", "LTC", "HASH11", "QBTC11", "BITH11",
            "ETHE11", "DEFI11", "CRPT11", "WEB311"
    );

    private static final Set<String> INTERNATIONAL_ETF_TICKERS = Set.of(
            "IVVB11", "SPXI11", "NASD11", "WRLD11", "ACWI11", "USTK11",
            "EURP11", "XINA11", "USAL11", "TECK11", "SHOT11"
    );

    private String inferAssetClass(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return "Outros";
        }
        String t = stripFractionalSuffix(ticker).trim().toUpperCase(Locale.ROOT);

        // Renda Fixa
        if (t.startsWith("TESOURO") || t.startsWith("CDB") || t.startsWith("LCI")
                || t.startsWith("LCA") || t.contains("DIRETO") || t.contains("DEBENTURE")) {
            return "Renda Fixa";
        }

        // Criptoativos (direto ou ETFs de cripto na B3)
        if (CRYPTO_TICKERS.contains(t) || t.endsWith("-USD") || t.endsWith("-BRL")) {
            return "Criptoativos";
        }

        // ETFs Internacionais listados na B3 (precedência antes de FIIs *11)
        if (INTERNATIONAL_ETF_TICKERS.contains(t)) {
            return "Internacional";
        }

        // FIIs (Imobiliários)
        if (t.length() == 6 && t.endsWith("11")) {
            return "FIIs";
        }

        // BDRs (Brazilian Depositary Receipts)
        if (t.matches("^[A-Z]{4}34$")) {
            return "BDRs";
        }

        // Ações ordinárias/preferenciais
        if (t.matches("^[A-Z]{4}[34568]$") || (t.length() == 5 && Character.isDigit(t.charAt(4)))) {
            return "Ações";
        }

        // Ações/ETFs Internacionais diretos (ex.: AAPL, MSFT, VOO, BRK.B)
        if (t.matches("^[A-Z]{1,5}(\\.[A-Z]{1,2})?$")) {
            return "Internacional";
        }

        return "Outros";
    }
}
