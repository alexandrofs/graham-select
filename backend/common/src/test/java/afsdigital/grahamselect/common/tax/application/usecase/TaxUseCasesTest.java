package afsdigital.grahamselect.common.tax.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.tax.application.dto.TaxAssetDeclarationItemDTO;
import afsdigital.grahamselect.common.tax.application.dto.TaxAssetDeclarationReportDTO;
import afsdigital.grahamselect.common.tax.application.dto.TaxAvailableYearsDTO;
import afsdigital.grahamselect.common.tax.domain.entities.TaxAssetGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxUseCasesTest {

    @Mock
    private TradePort tradePort;

    @Mock
    private CompanyRepository companyRepository;

    private GetTaxAssetDeclarationReportUseCase reportUseCase;
    private GetTaxAvailableYearsUseCase availableYearsUseCase;

    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        reportUseCase = new GetTaxAssetDeclarationReportUseCase(tradePort, companyRepository);
        availableYearsUseCase = new GetTaxAvailableYearsUseCase(tradePort);
        lenient().when(companyRepository.findAllByTickerIn(any())).thenAnswer(inv -> {
            Collection<String> tickers = inv.getArgument(0);
            if (tickers == null) {
                return List.of();
            }
            return tickers.stream()
                    .map(companyRepository::findByTicker)
                    .filter(Objects::nonNull)
                    .toList();
        });
    }

    private Trade createTrade(
            String ticker,
            String side,
            LocalDate date,
            BigDecimal qty,
            BigDecimal price,
            String broker,
            String profileId
    ) {
        return Trade.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .profileId(profileId)
                .ticker(ticker)
                .side(side)
                .tradeDate(date)
                .quantity(qty)
                .price(price)
                .broker(broker)
                .createdAt(LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 10, 0))
                .build();
    }

    @Test
    @DisplayName("Deve calcular preço médio e custo de aquisição acumulado com compras sucessivas e venda parcial")
    void shouldCalculateWeightedAveragePriceAndPartialSale() {
        // Ano-calendário: 2025 (Snapshot 31/12/2024 e 31/12/2025)
        // 2024-05-10: Compra 100 PETR4 a R$ 30,00 -> Total: 3.000,00, PM: 30,00
        // 2024-11-15: Compra 100 PETR4 a R$ 40,00 -> Total: 7.000,00, PM: 35,00
        // 31/12/2024: 200 ações, Custo: 7.000,00, PM: 35,00
        // 2025-03-20: Venda 50 PETR4 a R$ 45,00 -> Restam 150 ações, Custo: 150 * 35 = 5.250,00, PM: 35,00
        // 31/12/2025: 150 ações, Custo: 5.250,00, PM: 35,00
        Trade t1 = createTrade("PETR4", "COMPRA", LocalDate.of(2024, 5, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP Investimentos", null);
        Trade t2 = createTrade("PETR4", "COMPRA", LocalDate.of(2024, 11, 15), new BigDecimal("100"), new BigDecimal("40.00"), "XP Investimentos", null);
        Trade t3 = createTrade("PETR4", "VENDA", LocalDate.of(2025, 3, 20), new BigDecimal("50"), new BigDecimal("45.00"), "XP Investimentos", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2, t3));
        when(companyRepository.findByTicker("PETR4")).thenReturn(Company.builder()
                .ticker("PETR4")
                .name("PETROLEO BRASILEIRO S.A. PETROBRAS")
                .cnpj("33.000.167/0001-01")
                .assetClass("AÇÕES")
                .build());

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);

        assertThat(report.year()).isEqualTo(2025);
        assertThat(report.previousYear()).isEqualTo(2024);
        assertThat(report.previousYearTotal()).isEqualByComparingTo(new BigDecimal("7000.00"));
        assertThat(report.currentYearTotal()).isEqualByComparingTo(new BigDecimal("5250.00"));
        assertThat(report.items()).hasSize(1);

        TaxAssetDeclarationItemDTO item = report.items().get(0);
        assertThat(item.ticker()).isEqualTo("PETR4");
        assertThat(item.groupCode()).isEqualTo("03");
        assertThat(item.itemCode()).isEqualTo("01");
        assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(item.previousYearValue()).isEqualByComparingTo(new BigDecimal("7000.00"));
        assertThat(item.currentYearValue()).isEqualByComparingTo(new BigDecimal("5250.00"));
        assertThat(item.averagePrice()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(item.liquidated()).isFalse();
        assertThat(item.discrimination()).contains("150 ações de PETROLEO BRASILEIRO S.A. PETROBRAS (PETR4)");
        assertThat(item.discrimination()).contains("CNPJ: 33.000.167/0001-01");
        assertThat(item.discrimination()).contains("Custodiado na corretora XP Investimentos");
        assertThat(item.discrimination()).contains("Custo total de aquisição em 31/12/2025: R$ 5.250,00");
    }

    @Test
    @DisplayName("Deve incluir ativo totalmente alienado durante o ano com situação anterior preenchida e atual 0.00")
    void shouldIncludeFullyLiquidatedAssetDuringYear() {
        // 2024-06-01: Compra 200 VALE3 a R$ 60,00 -> Total: 12.000,00
        // 2025-08-10: Venda 200 VALE3 a R$ 70,00 -> Liquidado totalmente
        Trade t1 = createTrade("VALE3", "COMPRA", LocalDate.of(2024, 6, 1), new BigDecimal("200"), new BigDecimal("60.00"), "BTG Pactual", null);
        Trade t2 = createTrade("VALE3", "VENDA", LocalDate.of(2025, 8, 10), new BigDecimal("200"), new BigDecimal("70.00"), "BTG Pactual", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2));
        when(companyRepository.findByTicker("VALE3")).thenReturn(Company.builder()
                .ticker("VALE3")
                .name("VALE S.A.")
                .cnpj("33.592.510/0001-54")
                .assetClass("AÇÕES")
                .build());

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);

        assertThat(report.items()).hasSize(1);
        TaxAssetDeclarationItemDTO item = report.items().get(0);
        assertThat(item.ticker()).isEqualTo("VALE3");
        assertThat(item.liquidated()).isTrue();
        assertThat(item.previousYearValue()).isEqualByComparingTo(new BigDecimal("12000.00"));
        assertThat(item.currentYearValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.discrimination()).contains("Posição de 200 ações de VALE S.A. (VALE3)");
        assertThat(item.discrimination()).contains("totalmente alienada durante o ano-calendário de 2025");
        assertThat(item.discrimination()).contains("Custo total de aquisição em 31/12/2025: R$ 0,00");
    }

    @Test
    @DisplayName("Não deve incluir ativo comprado e totalmente vendido no mesmo ano (ambos saldos zerados)")
    void shouldExcludeAssetBoughtAndSoldInSameYear() {
        Trade t1 = createTrade("ITUB4", "COMPRA", LocalDate.of(2025, 2, 10), new BigDecimal("100"), new BigDecimal("32.00"), "XP", null);
        Trade t2 = createTrade("ITUB4", "VENDA", LocalDate.of(2025, 5, 10), new BigDecimal("100"), new BigDecimal("35.00"), "XP", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2));

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);

        assertThat(report.items()).isEmpty();
        assertThat(report.currentYearTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.previousYearTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve consolidar ativos fracionários (PETR4F com PETR4)")
    void shouldConsolidateFractionalTickers() {
        Trade t1 = createTrade("PETR4", "COMPRA", LocalDate.of(2025, 1, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP", null);
        Trade t2 = createTrade("PETR4F", "COMPRA", LocalDate.of(2025, 2, 10), new BigDecimal("50"), new BigDecimal("30.00"), "XP", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2));
        when(companyRepository.findByTicker("PETR4")).thenReturn(Company.builder()
                .ticker("PETR4")
                .name("PETROLEO BRASILEIRO S.A. PETROBRAS")
                .cnpj("33.000.167/0001-01")
                .build());

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);

        assertThat(report.items()).hasSize(1);
        TaxAssetDeclarationItemDTO item = report.items().get(0);
        assertThat(item.ticker()).isEqualTo("PETR4");
        assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(item.currentYearValue()).isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    @DisplayName("Deve classificar FII, ETF e BDR com códigos e grupos corretos")
    void shouldClassifyAssetGroupsCorrectly() {
        Trade tFii = createTrade("HGLG11", "COMPRA", LocalDate.of(2025, 1, 15), new BigDecimal("10"), new BigDecimal("160.00"), "Clear", null);
        Trade tEtf = createTrade("BOVA11", "COMPRA", LocalDate.of(2025, 2, 15), new BigDecimal("20"), new BigDecimal("110.00"), "Clear", null);
        Trade tBdr = createTrade("AAPL34", "COMPRA", LocalDate.of(2025, 3, 15), new BigDecimal("30"), new BigDecimal("50.00"), "Clear", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(tFii, tEtf, tBdr));
        when(companyRepository.findByTicker("HGLG11")).thenReturn(Company.builder().ticker("HGLG11").name("CSHG Logística").assetClass("FII").build());
        when(companyRepository.findByTicker("BOVA11")).thenReturn(Company.builder().ticker("BOVA11").name("iShares Ibovespa").assetClass("ETF").build());
        when(companyRepository.findByTicker("AAPL34")).thenReturn(Company.builder().ticker("AAPL34").name("Apple Inc.").assetClass("BDR").build());

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);

        assertThat(report.items()).hasSize(3);

        TaxAssetDeclarationItemDTO bdrItem = report.items().stream().filter(i -> i.ticker().equals("AAPL34")).findFirst().orElseThrow();
        assertThat(bdrItem.groupCode()).isEqualTo("04");
        assertThat(bdrItem.itemCode()).isEqualTo("04");

        TaxAssetDeclarationItemDTO fiiItem = report.items().stream().filter(i -> i.ticker().equals("HGLG11")).findFirst().orElseThrow();
        assertThat(fiiItem.groupCode()).isEqualTo("07");
        assertThat(fiiItem.itemCode()).isEqualTo("03");

        TaxAssetDeclarationItemDTO etfItem = report.items().stream().filter(i -> i.ticker().equals("BOVA11")).findFirst().orElseThrow();
        assertThat(etfItem.groupCode()).isEqualTo("07");
        assertThat(etfItem.itemCode()).isEqualTo("09");
    }

    @Test
    @DisplayName("Deve formatar texto de custódia com múltiplas corretoras em ordem alfabética")
    void shouldFormatMultipleBrokersInAlphabeticalOrder() {
        Trade t1 = createTrade("PETR4", "COMPRA", LocalDate.of(2025, 1, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP Investimentos", null);
        Trade t2 = createTrade("PETR4", "COMPRA", LocalDate.of(2025, 2, 10), new BigDecimal("100"), new BigDecimal("30.00"), "BTG Pactual", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2));

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);

        assertThat(report.items()).hasSize(1);
        TaxAssetDeclarationItemDTO item = report.items().get(0);
        assertThat(item.discrimination()).contains("Custodiado nas corretoras: BTG Pactual, XP Investimentos");
    }

    @Test
    @DisplayName("Deve isolar transações de dependente familiar pelo profileId")
    void shouldFilterByProfileId() {
        String profileId = "profile-dependent-1";
        Trade tMaster = createTrade("PETR4", "COMPRA", LocalDate.of(2025, 1, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP", null);
        Trade tDep = createTrade("VALE3", "COMPRA", LocalDate.of(2025, 1, 10), new BigDecimal("50"), new BigDecimal("60.00"), "XP", profileId);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(tMaster, tDep));

        TaxAssetDeclarationReportDTO reportMaster = reportUseCase.execute(userId, 2025, null);
        assertThat(reportMaster.items()).hasSize(1);
        assertThat(reportMaster.items().get(0).ticker()).isEqualTo("PETR4");

        TaxAssetDeclarationReportDTO reportDep = reportUseCase.execute(userId, 2025, profileId);
        assertThat(reportDep.items()).hasSize(1);
        assertThat(reportDep.items().get(0).ticker()).isEqualTo("VALE3");
    }

    @Test
    @DisplayName("Deve retornar lista ordenada decrescente dos anos disponíveis incluindo anos em custódia até o ano corrente")
    void shouldReturnAvailableYearsInDescendingOrder() {
        Trade t1 = createTrade("PETR4", "COMPRA", LocalDate.of(2023, 5, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP", null);
        Trade t2 = createTrade("PETR4", "COMPRA", LocalDate.of(2025, 1, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP", null);
        Trade t3 = createTrade("PETR4", "COMPRA", LocalDate.of(2024, 7, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2, t3));

        TaxAvailableYearsDTO availableYears = availableYearsUseCase.execute(userId, null);

        assertThat(availableYears.years()).contains(2025, 2024, 2023);
        assertThat(availableYears.years()).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("Deve classificar Units da B3 como Ações e ETFs conhecidos como ETF")
    void shouldClassifyUnitsAndKnownEtfsCorrectly() {
        assertThat(TaxAssetGroup.classify(null, "TAEE11")).isEqualTo(TaxAssetGroup.ACOES);
        assertThat(TaxAssetGroup.classify(null, "SANB11")).isEqualTo(TaxAssetGroup.ACOES);
        assertThat(TaxAssetGroup.classify(null, "BOVA11")).isEqualTo(TaxAssetGroup.ETF);
        assertThat(TaxAssetGroup.classify(null, "HGLG11")).isEqualTo(TaxAssetGroup.FII);

        Company agroCompany = Company.builder()
                .ticker("AGRO3")
                .name("SLC AGRICOLA")
                .assetClass("AGRONEGÓCIO")
                .build();
        assertThat(TaxAssetGroup.classify(agroCompany, "AGRO3")).isEqualTo(TaxAssetGroup.ACOES);
    }

    @Test
    @DisplayName("Deve calcular subtotais por grupo e manter caixa alta de corretora em alienação")
    void shouldCalculateSubtotalsAndPreserveBrokerCasing() {
        Trade t1 = createTrade("PETR4", "COMPRA", LocalDate.of(2024, 5, 10), new BigDecimal("100"), new BigDecimal("30.00"), "XP Investimentos", null);
        Trade t2 = createTrade("PETR4", "VENDA", LocalDate.of(2025, 3, 20), new BigDecimal("100"), new BigDecimal("45.00"), "XP Investimentos", null);

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(t1, t2));
        when(companyRepository.findByTicker("PETR4")).thenReturn(Company.builder()
                .ticker("PETR4")
                .name("PETROLEO BRASILEIRO S.A. PETROBRAS")
                .cnpj("33.000.167/0001-01")
                .assetClass("AÇÕES")
                .build());

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);
        assertThat(report.subtotalsByGroup()).isNotNull();
        assertThat(report.items().get(0).discrimination()).contains("custodiado na corretora XP Investimentos");
    }

    @Test
    @DisplayName("Deve ordenar COMPRA antes de VENDA em trades do mesmo dia com timestamps idênticos")
    void shouldOrderBuyBeforeSellOnSameDay() {
        LocalDate sameDay = LocalDate.of(2025, 6, 1);
        LocalDateTime sameTime = LocalDateTime.of(2025, 6, 1, 10, 0);

        Trade tSell = Trade.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .ticker("VALE3")
                .side("VENDA")
                .tradeDate(sameDay)
                .quantity(new BigDecimal("50"))
                .price(new BigDecimal("60.00"))
                .createdAt(sameTime)
                .build();

        Trade tBuy = Trade.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .ticker("VALE3")
                .side("COMPRA")
                .tradeDate(sameDay)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("50.00"))
                .createdAt(sameTime)
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(tSell, tBuy));
        when(companyRepository.findByTicker("VALE3")).thenReturn(Company.builder()
                .ticker("VALE3")
                .name("VALE S.A.")
                .cnpj("33.592.510/0001-54")
                .assetClass("AÇÕES")
                .build());

        TaxAssetDeclarationReportDTO report = reportUseCase.execute(userId, 2025, null);
        assertThat(report.items()).hasSize(1);
        TaxAssetDeclarationItemDTO item = report.items().get(0);
        assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(item.averagePrice()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

}
