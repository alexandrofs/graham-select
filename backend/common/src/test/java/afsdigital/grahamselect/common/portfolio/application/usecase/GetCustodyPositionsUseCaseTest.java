package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.application.repository.StockPricePort;
import afsdigital.grahamselect.valuation.domain.entities.StockPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCustodyPositionsUseCaseTest {

    @Mock
    private TradePort tradePort;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StockPricePort stockPricePort;

    private GetCustodyPositionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCustodyPositionsUseCase(tradePort, companyRepository, stockPricePort);
    }

    @Test
    void shouldCalculateCustodyPositionsCorrectly() {
        String userId = "user-1";
        String ticker = "PETR4";
        String companyId = "comp-1";

        // Trade 1: COMPRA de 10 PETR4 a R$ 30.00
        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .build();

        // Trade 2: COMPRA de 5 PETR4 a R$ 33.00
        // Preço médio deve ser: (10 * 30 + 5 * 33) / 15 = 495 / 15 = R$ 33.00 (espera, na verdade: (300 + 165) / 15 = 465 / 15 = R$ 31.00)
        Trade trade2 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("33.00"))
                .build();

        // Trade 3: VENDA de 3 PETR4 a R$ 35.00 (quantidade final: 12)
        Trade trade3 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.VENDA.name())
                .quantity(new BigDecimal("3"))
                .price(new BigDecimal("35.00"))
                .build();

        // Trade 4: DIVIDENDO de PETR4 R$ 1.50 (não afeta posição)
        Trade trade4 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.DIVIDENDO.name())
                .quantity(new BigDecimal("12"))
                .price(new BigDecimal("1.50"))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade1, trade2, trade3, trade4));

        Company company = new Company(companyId, ticker);
        when(companyRepository.findByTicker(ticker)).thenReturn(company);

        // Preço atual = R$ 34.10, data de hoje em UTC (LIVE)
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        StockPrice stockPrice = StockPrice.builder()
                .companyId(companyId)
                .price(new BigDecimal("34.10"))
                .date(today)
                .build();
        when(stockPricePort.findLatestByCompanyIds(List.of(companyId))).thenReturn(Map.of(companyId, stockPrice));

        List<CustodyPositionDTO> result = useCase.execute(userId);

        assertEquals(1, result.size());
        CustodyPositionDTO pos = result.get(0);

        assertEquals(ticker, pos.ticker());
        assertEquals(new BigDecimal("12.00"), pos.quantity());
        assertEquals(new BigDecimal("31.00"), pos.averagePrice());
        assertEquals(new BigDecimal("34.10"), pos.currentPrice());
        assertEquals(new BigDecimal("409.20"), pos.marketValue()); // 12 * 34.10 = 409.20
        assertEquals(new BigDecimal("10.00"), pos.gainLossPercentage()); // ((34.10 - 31.00) / 31.00) * 100 = 10%
        assertEquals("LIVE", pos.priceSource());
        assertEquals(today.atStartOfDay(ZoneOffset.UTC).toInstant(), pos.priceUpdatedAt());
        assertEquals("Ações", pos.assetClass());
    }

    @Test
    void shouldExcludeZeroOrNegativePositions() {
        String userId = "user-1";
        String ticker = "VALE3";

        // Trade 1: COMPRA de 10 VALE3
        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("80.00"))
                .build();

        // Trade 2: VENDA de 10 VALE3 (zerando a posição)
        Trade trade2 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.VENDA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("85.00"))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade1, trade2));

        List<CustodyPositionDTO> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFallbackToCacheAndAveragePriceWhenStockPriceNotFound() {
        String userId = "user-1";
        String ticker = "ITUB4";
        String companyId = "comp-2";

        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("28.50"))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade1));

        Company company = new Company(companyId, ticker);
        when(companyRepository.findByTicker(ticker)).thenReturn(company);
        
        // Stock Price não encontrada no map retornado
        when(stockPricePort.findLatestByCompanyIds(List.of(companyId))).thenReturn(Collections.emptyMap());

        List<CustodyPositionDTO> result = useCase.execute(userId);

        assertEquals(1, result.size());
        CustodyPositionDTO pos = result.get(0);

        assertEquals(ticker, pos.ticker());
        assertEquals(new BigDecimal("10.00"), pos.quantity());
        assertEquals(new BigDecimal("28.50"), pos.averagePrice());
        assertEquals(new BigDecimal("28.50"), pos.currentPrice()); // fallback para averagePrice
        assertEquals(new BigDecimal("285.00"), pos.marketValue()); // 10 * 28.50
        assertEquals(new BigDecimal("0.00"), pos.gainLossPercentage());
        assertEquals("CACHE", pos.priceSource());
        assertNull(pos.priceUpdatedAt());
    }

    @Test
    void shouldMarkAsCacheIfStockPriceIsOld() {
        String userId = "user-1";
        String ticker = "ITUB4";
        String companyId = "comp-2";

        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("28.50"))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade1));

        Company company = new Company(companyId, ticker);
        when(companyRepository.findByTicker(ticker)).thenReturn(company);

        // Preço antigo (ontem ou anterior) em UTC (CACHE)
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        StockPrice stockPrice = StockPrice.builder()
                .companyId(companyId)
                .price(new BigDecimal("30.00"))
                .date(yesterday)
                .build();
        when(stockPricePort.findLatestByCompanyIds(List.of(companyId))).thenReturn(Map.of(companyId, stockPrice));

        List<CustodyPositionDTO> result = useCase.execute(userId);

        assertEquals(1, result.size());
        CustodyPositionDTO pos = result.get(0);
        assertEquals("CACHE", pos.priceSource());
        assertEquals(yesterday.atStartOfDay(ZoneOffset.UTC).toInstant(), pos.priceUpdatedAt());
    }

    @Test
    void shouldResetAveragePriceWhenPositionIsZeroedAndReboughtAndRespectChronologicalOrder() {
        String userId = "user-1";
        String ticker = "PETR4";
        String companyId = "comp-1";

        // Adicionados FORA de ordem cronologica para testar a ordenacao automatica do UseCase
        // Trade 3: Recompra cronologicamente em data posterior (30/05)
        Trade trade3 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("35.00"))
                .tradeDate(LocalDate.of(2026, 5, 30))
                .build();

        // Trade 1: Primeira compra cronologicamente (10/05)
        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .tradeDate(LocalDate.of(2026, 5, 10))
                .build();

        // Trade 2: Venda cronologicamente que zera a posicao (20/05)
        Trade trade2 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.VENDA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("32.00"))
                .tradeDate(LocalDate.of(2026, 5, 20))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade3, trade1, trade2));

        Company company = new Company(companyId, ticker);
        when(companyRepository.findByTicker(ticker)).thenReturn(company);

        LocalDate today = LocalDate.of(2026, 5, 30);
        StockPrice stockPrice = StockPrice.builder()
                .companyId(companyId)
                .price(new BigDecimal("40.00"))
                .date(today)
                .build();
        when(stockPricePort.findLatestByCompanyIds(List.of(companyId))).thenReturn(Map.of(companyId, stockPrice));

        List<CustodyPositionDTO> result = useCase.execute(userId);

        assertEquals(1, result.size());
        CustodyPositionDTO pos = result.get(0);

        assertEquals(ticker, pos.ticker());
        // A quantidade deve ser 5 (da recompra posterior)
        assertEquals(new BigDecimal("5.00"), pos.quantity());
        // O preco medio deve ser R$ 35.00 (apenas o preco da recompra, porque as anteriores foram liquidadas!)
        assertEquals(new BigDecimal("35.00"), pos.averagePrice());
        assertEquals(new BigDecimal("40.00"), pos.currentPrice());
        assertEquals(new BigDecimal("200.00"), pos.marketValue()); // 5 * 40 = 200
        assertEquals(new BigDecimal("14.29"), pos.gainLossPercentage()); // ((40 - 35) / 35) * 100 = 14.2857% -> 14.29%
    }

    @Test
    void shouldInferAssetClassForFractionalTickersCorrectly() {
        String userId = "user-1";
        String ticker = "ITUB4F"; // Fracionário de ITUB4
        String companyId = "comp-3";

        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("25.00"))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade1));

        Company company = new Company(companyId, ticker);
        when(companyRepository.findByTicker(ticker)).thenReturn(company);

        StockPrice stockPrice = StockPrice.builder()
                .companyId(companyId)
                .price(new BigDecimal("25.00"))
                .date(LocalDate.now(ZoneOffset.UTC))
                .build();
        when(stockPricePort.findLatestByCompanyIds(List.of(companyId))).thenReturn(Map.of(companyId, stockPrice));

        List<CustodyPositionDTO> result = useCase.execute(userId);

        assertEquals(1, result.size());
        assertEquals("Ações", result.get(0).assetClass());
    }

    @Test
    void shouldInferAssetClassForAllCategoriesCorrectly() {
        String userId = "user-1";
        
        // Trades para cada tipo de ativo
        Trade t1 = Trade.builder().userId(userId).ticker("VALE3").side(TradeSide.COMPRA.name()).quantity(BigDecimal.ONE).price(BigDecimal.TEN).build();
        Trade t2 = Trade.builder().userId(userId).ticker("HGLG11").side(TradeSide.COMPRA.name()).quantity(BigDecimal.ONE).price(BigDecimal.TEN).build();
        Trade t3 = Trade.builder().userId(userId).ticker("AAPL34").side(TradeSide.COMPRA.name()).quantity(BigDecimal.ONE).price(BigDecimal.TEN).build();
        Trade t4 = Trade.builder().userId(userId).ticker("CDB_PRE_12").side(TradeSide.COMPRA.name()).quantity(BigDecimal.ONE).price(BigDecimal.TEN).build();
        Trade t5 = Trade.builder().userId(userId).ticker("TESOURO_IPCA").side(TradeSide.COMPRA.name()).quantity(BigDecimal.ONE).price(BigDecimal.TEN).build();
        Trade t6 = Trade.builder().userId(userId).ticker("OUTRO_ATIVO").side(TradeSide.COMPRA.name()).quantity(BigDecimal.ONE).price(BigDecimal.TEN).build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2, t3, t4, t5, t6));

        // Mock Companies
        Company c1 = new Company("id-vale", "VALE3");
        Company c2 = new Company("id-hglg", "HGLG11");
        Company c3 = new Company("id-aapl", "AAPL34");
        Company c4 = new Company("id-cdb", "CDB_PRE_12");
        Company c5 = new Company("id-tes", "TESOURO_IPCA");
        Company c6 = new Company("id-out", "OUTRO_ATIVO");

        when(companyRepository.findByTicker("VALE3")).thenReturn(c1);
        when(companyRepository.findByTicker("HGLG11")).thenReturn(c2);
        when(companyRepository.findByTicker("AAPL34")).thenReturn(c3);
        when(companyRepository.findByTicker("CDB_PRE_12")).thenReturn(c4);
        when(companyRepository.findByTicker("TESOURO_IPCA")).thenReturn(c5);
        when(companyRepository.findByTicker("OUTRO_ATIVO")).thenReturn(c6);

        // Mock Stock Prices
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(stockPricePort.findLatestByCompanyIds(anyList())).thenReturn(Map.of(
            "id-vale", StockPrice.builder().companyId("id-vale").price(BigDecimal.TEN).date(today).build(),
            "id-hglg", StockPrice.builder().companyId("id-hglg").price(BigDecimal.TEN).date(today).build(),
            "id-aapl", StockPrice.builder().companyId("id-aapl").price(BigDecimal.TEN).date(today).build(),
            "id-cdb", StockPrice.builder().companyId("id-cdb").price(BigDecimal.TEN).date(today).build(),
            "id-tes", StockPrice.builder().companyId("id-tes").price(BigDecimal.TEN).date(today).build(),
            "id-out", StockPrice.builder().companyId("id-out").price(BigDecimal.TEN).date(today).build()
        ));

        List<CustodyPositionDTO> result = useCase.execute(userId);

        Map<String, String> inferredClasses = result.stream()
                .collect(Collectors.toMap(CustodyPositionDTO::ticker, CustodyPositionDTO::assetClass));

        assertEquals(6, result.size());
        assertEquals("Ações", inferredClasses.get("VALE3"));
        assertEquals("FIIs", inferredClasses.get("HGLG11"));
        assertEquals("BDRs", inferredClasses.get("AAPL34"));
        assertEquals("Renda Fixa", inferredClasses.get("CDB_PRE_12"));
        assertEquals("Renda Fixa", inferredClasses.get("TESOURO_IPCA"));
        assertEquals("Outros", inferredClasses.get("OUTRO_ATIVO"));
    }
}
