package afsdigital.grahamselect.common.tax.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.tax.application.dto.AnnualTaxSummaryDTO;
import afsdigital.grahamselect.common.tax.application.dto.MonthlyTaxCalculationDTO;
import afsdigital.grahamselect.common.tax.domain.entities.DarfStatus;
import afsdigital.grahamselect.common.tax.domain.utils.DarfDueDateCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxMonthlyCapitalGainsTest {

    @Mock
    private TradePort tradePort;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private GetTaxMonthlyCapitalGainsUseCase useCase;

    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        lenient().when(companyRepository.findByTicker(anyString())).thenAnswer(invocation -> {
            String ticker = invocation.getArgument(0);
            if ("HGLG11".equals(ticker) || "XPLG11".equals(ticker)) {
                return Company.builder().ticker(ticker).assetClass("FII").build();
            }
            if ("BOVA11".equals(ticker) || "IVVB11".equals(ticker)) {
                return Company.builder().ticker(ticker).assetClass("ETF").build();
            }
            if ("AAPL34".equals(ticker)) {
                return Company.builder().ticker(ticker).assetClass("BDR").build();
            }
            return Company.builder().ticker(ticker).assetClass("AÇÕES").build();
        });
    }

    private Trade buildTrade(String ticker, LocalDate tradeDate, String side, BigDecimal qty, BigDecimal price) {
        return Trade.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .ticker(ticker)
                .tradeDate(tradeDate)
                .side(side)
                .quantity(qty)
                .price(price)
                .broker("XP Investimentos")
                .createdAt(LocalDateTime.of(tradeDate.getYear(), tradeDate.getMonth(), tradeDate.getDayOfMonth(), 10, 0))
                .build();
    }

    @Test
    @DisplayName("Cenário A: Alienação de ações <= R$ 20.000,00 com lucro -> Lucro Isento, imposto zero")
    void scenarioA_equityUnder20kWithProfit_isExempt() {
        List<Trade> trades = List.of(
                buildTrade("PETR4", LocalDate.of(2025, 1, 10), "COMPRA", new BigDecimal("100"), new BigDecimal("30.00")),
                buildTrade("PETR4", LocalDate.of(2025, 3, 15), "VENDA", new BigDecimal("100"), new BigDecimal("40.00")) // Venda R$ 4.000, Lucro R$ 1.000
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        assertThat(summary.year()).isEqualTo(2025);
        MonthlyTaxCalculationDTO march = summary.monthlyCalculations().get(2); // Mês 3 (Março)
        assertThat(march.month()).isEqualTo(3);
        assertThat(march.totalEquitySales()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(march.isEquityExempt()).isTrue();
        assertThat(march.exemptProfit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(march.commonTaxableProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(march.commonTaxDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(march.darfStatus()).isEqualTo(DarfStatus.EXEMPT);
        assertThat(march.darfAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Cenário B: Alienação de ações <= R$ 20.000,00 com lucro com prejuízo acumulado anterior -> Prejuízo NÃO é consumido")
    void scenarioB_equityUnder20k_doesNotConsumePreviousLoss() {
        List<Trade> trades = List.of(
                // Janeiro: prejuízo de R$ 2.000 na cesta comum
                buildTrade("VALE3", LocalDate.of(2025, 1, 5), "COMPRA", new BigDecimal("100"), new BigDecimal("60.00")),
                buildTrade("VALE3", LocalDate.of(2025, 1, 20), "VENDA", new BigDecimal("100"), new BigDecimal("40.00")), // Perda R$ 2.000
                // Fevereiro: lucro isento de R$ 500 em PETR4 (venda R$ 2.500)
                buildTrade("PETR4", LocalDate.of(2025, 2, 1), "COMPRA", new BigDecimal("100"), new BigDecimal("20.00")),
                buildTrade("PETR4", LocalDate.of(2025, 2, 25), "VENDA", new BigDecimal("100"), new BigDecimal("25.00")) // Venda R$ 2.500, Lucro R$ 500
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO jan = summary.monthlyCalculations().get(0);
        assertThat(jan.commonLossCarryforward()).isEqualByComparingTo(new BigDecimal("2000.00"));

        MonthlyTaxCalculationDTO feb = summary.monthlyCalculations().get(1);
        assertThat(feb.isEquityExempt()).isTrue();
        assertThat(feb.exemptProfit()).isEqualByComparingTo(new BigDecimal("500.00"));
        // Prejuízo anterior NÃO deve ser reduzido pelo lucro isento!
        assertThat(feb.commonLossCompensated()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.commonLossCarryforward()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("Cenário C: Alienação de ações <= R$ 20.000,00 com prejuízo -> Prejuízo acumula na cesta comum")
    void scenarioC_equityUnder20kWithLoss_accumulatesToCommonLoss() {
        List<Trade> trades = List.of(
                buildTrade("BBDC4", LocalDate.of(2025, 4, 10), "COMPRA", new BigDecimal("200"), new BigDecimal("15.00")),
                buildTrade("BBDC4", LocalDate.of(2025, 4, 25), "VENDA", new BigDecimal("200"), new BigDecimal("12.00")) // Venda R$ 2.400, Prejuízo R$ 600
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO april = summary.monthlyCalculations().get(3);
        assertThat(april.totalEquitySales()).isEqualByComparingTo(new BigDecimal("2400.00"));
        assertThat(april.isEquityExempt()).isTrue();
        assertThat(april.exemptProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(april.commonLossCarryforward()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(april.darfStatus()).isEqualTo(DarfStatus.LOSS);
    }

    @Test
    @DisplayName("Cenário D: Alienação de ações > R$ 20.000,00 com lucro tributável à alíquota de 15%")
    void scenarioD_equityOver20kWithProfit_taxedAt15Percent() {
        List<Trade> trades = List.of(
                buildTrade("WEGE3", LocalDate.of(2025, 5, 2), "COMPRA", new BigDecimal("1000"), new BigDecimal("20.00")),
                buildTrade("WEGE3", LocalDate.of(2025, 5, 20), "VENDA", new BigDecimal("1000"), new BigDecimal("30.00")) // Venda R$ 30.000, Lucro R$ 10.000
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO may = summary.monthlyCalculations().get(4);
        assertThat(may.totalEquitySales()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(may.isEquityExempt()).isFalse();
        assertThat(may.exemptProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(may.commonTaxableProfit()).isEqualByComparingTo(new BigDecimal("10000.00"));
        // Imposto 15% de 10.000 = 1.500
        assertThat(may.commonTaxDue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(may.darfStatus()).isEqualTo(DarfStatus.DUE);
        assertThat(may.darfAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(may.darfDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Cenário E: Compensação de prejuízo anterior da cesta comum sobre lucro tributável")
    void scenarioE_compensationOfPreviousLossOnTaxableProfit() {
        List<Trade> trades = List.of(
                // Janeiro: prejuízo de R$ 4.000 em ações
                buildTrade("MGLU3", LocalDate.of(2025, 1, 5), "COMPRA", new BigDecimal("1000"), new BigDecimal("10.00")),
                buildTrade("MGLU3", LocalDate.of(2025, 1, 20), "VENDA", new BigDecimal("1000"), new BigDecimal("6.00")), // Perda R$ 4.000
                // Março: ETF BOVA11 com lucro de R$ 6.000 (ETF não tem isenção)
                buildTrade("BOVA11", LocalDate.of(2025, 3, 1), "COMPRA", new BigDecimal("100"), new BigDecimal("100.00")),
                buildTrade("BOVA11", LocalDate.of(2025, 3, 28), "VENDA", new BigDecimal("100"), new BigDecimal("160.00")) // Lucro R$ 6.000
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO march = summary.monthlyCalculations().get(2);
        assertThat(march.commonTaxableProfit()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(march.commonLossCompensated()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(march.commonLossCarryforward()).isEqualByComparingTo(BigDecimal.ZERO);
        // Base tributável = 6.000 - 4.000 = 2.000. Imposto 15% = 300,00
        assertThat(march.commonTaxDue()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(march.darfStatus()).isEqualTo(DarfStatus.DUE);
        assertThat(march.darfAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Cenário F: Alienação de FII com lucro tributado a 20%, sem isenção de R$ 20k")
    void scenarioF_fiiWithProfit_taxedAt20PercentWithoutExemption() {
        List<Trade> trades = List.of(
                // Venda total de apenas R$ 5.000 (abaixo de 20k), mas FII não tem isenção!
                buildTrade("HGLG11", LocalDate.of(2025, 6, 5), "COMPRA", new BigDecimal("50"), new BigDecimal("160.00")),
                buildTrade("HGLG11", LocalDate.of(2025, 6, 20), "VENDA", new BigDecimal("50"), new BigDecimal("180.00")) // Custo 8.000, Venda 9.000, Lucro 1.000
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO june = summary.monthlyCalculations().get(5);
        assertThat(june.fiiTaxableProfit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        // 20% de 1.000 = 200,00
        assertThat(june.fiiTaxDue()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(june.darfStatus()).isEqualTo(DarfStatus.DUE);
        assertThat(june.darfAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Cenário G: Prejuízo de FII NÃO compensa ações e prejuízo de ações NÃO compensa FII")
    void scenarioG_strictSegregationBetweenFiiAndCommon() {
        List<Trade> trades = List.of(
                // Janeiro: Prejuízo de R$ 3.000 em FII
                buildTrade("HGLG11", LocalDate.of(2025, 1, 10), "COMPRA", new BigDecimal("100"), new BigDecimal("170.00")),
                buildTrade("HGLG11", LocalDate.of(2025, 1, 20), "VENDA", new BigDecimal("100"), new BigDecimal("140.00")), // Perda FII R$ 3.000
                // Fevereiro: Lucro tributável de R$ 5.000 em ações (> 20k venda)
                buildTrade("VALE3", LocalDate.of(2025, 2, 5), "COMPRA", new BigDecimal("500"), new BigDecimal("50.00")),
                buildTrade("VALE3", LocalDate.of(2025, 2, 25), "VENDA", new BigDecimal("500"), new BigDecimal("60.00")) // Venda 30k, Lucro 5.000
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO jan = summary.monthlyCalculations().get(0);
        assertThat(jan.fiiLossCarryforward()).isEqualByComparingTo(new BigDecimal("3000.00"));

        MonthlyTaxCalculationDTO feb = summary.monthlyCalculations().get(1);
        // O lucro de ações NÃO deve ser compensado com prejuízo de FII!
        assertThat(feb.commonTaxableProfit()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(feb.commonLossCompensated()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.commonTaxDue()).isEqualByComparingTo(new BigDecimal("750.00")); // 15% de 5000
        assertThat(feb.fiiLossCarryforward()).isEqualByComparingTo(new BigDecimal("3000.00")); // Continua intacto
    }

    @Test
    @DisplayName("Cenário H: Imposto < R$ 10,00 acumula para o mês seguinte até atingir >= R$ 10,00")
    void scenarioH_taxUnder10_accumulatesToNextMonth() {
        List<Trade> trades = List.of(
                // Mês 1: Lucro de R$ 40 em ETF (15% = R$ 6,00 de imposto) -> Menor que R$ 10
                buildTrade("BOVA11", LocalDate.of(2025, 1, 10), "COMPRA", new BigDecimal("10"), new BigDecimal("100.00")),
                buildTrade("BOVA11", LocalDate.of(2025, 1, 25), "VENDA", new BigDecimal("10"), new BigDecimal("104.00")), // Lucro R$ 40 -> Imposto R$ 6,00
                // Mês 2: Lucro de R$ 40 em ETF (15% = R$ 6,00 de imposto) -> Total acumulado = 12,00 >= 10 -> DARF gerado
                buildTrade("BOVA11", LocalDate.of(2025, 2, 10), "COMPRA", new BigDecimal("10"), new BigDecimal("100.00")),
                buildTrade("BOVA11", LocalDate.of(2025, 2, 25), "VENDA", new BigDecimal("10"), new BigDecimal("104.00")) // Lucro R$ 40 -> Imposto R$ 6,00
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO jan = summary.monthlyCalculations().get(0);
        assertThat(jan.totalTaxDue()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(jan.accumulatedTaxUnder10()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(jan.darfStatus()).isEqualTo(DarfStatus.ACCUMULATED_BELOW_MINIMUM);
        assertThat(jan.darfAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        MonthlyTaxCalculationDTO feb = summary.monthlyCalculations().get(1);
        assertThat(feb.totalTaxDue()).isEqualByComparingTo(new BigDecimal("6.00"));
        // Total a recolher = 6,00 + 6,00 = 12,00 >= 10,00
        assertThat(feb.accumulatedTaxUnder10()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.darfStatus()).isEqualTo(DarfStatus.DUE);
        assertThat(feb.darfAmount()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("Cenário I: Carryover de prejuízo e saldo de DARF atravessando a virada de ano")
    void scenarioI_carryoverAcrossYearBoundary() {
        List<Trade> trades = List.of(
                // Dezembro/2024: Prejuízo de R$ 5.000 em ações
                buildTrade("PETR4", LocalDate.of(2024, 12, 1), "COMPRA", new BigDecimal("500"), new BigDecimal("30.00")),
                buildTrade("PETR4", LocalDate.of(2024, 12, 20), "VENDA", new BigDecimal("500"), new BigDecimal("20.00")), // Perda 5.000
                // Janeiro/2025: Venda de R$ 40.000 com lucro de R$ 3.000 em ações
                buildTrade("VALE3", LocalDate.of(2025, 1, 5), "COMPRA", new BigDecimal("1000"), new BigDecimal("37.00")),
                buildTrade("VALE3", LocalDate.of(2025, 1, 20), "VENDA", new BigDecimal("1000"), new BigDecimal("40.00")) // Lucro 3.000
        );
        when(tradePort.findAllByUserId(userId)).thenReturn(trades);

        AnnualTaxSummaryDTO summary = useCase.execute(userId, 2025, null);

        MonthlyTaxCalculationDTO jan = summary.monthlyCalculations().get(0);
        assertThat(jan.commonTaxableProfit()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(jan.commonLossCompensated()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(jan.commonLossCarryforward()).isEqualByComparingTo(new BigDecimal("2000.00")); // 5000 - 3000
        assertThat(jan.commonTaxDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jan.darfStatus()).isEqualTo(DarfStatus.NO_TAX_DUE);
    }

    @Test
    @DisplayName("Cenário J: Vencimento do DARF no último dia útil do mês subsequente")
    void scenarioJ_darfDueDateCalculation() {
        // Fevereiro de 2024 (ano bissexto): próximo mês é Março/2024. 31/03/2024 foi domingo -> recua para 28/03/2024 (29/03 é sexta-feira santa, mas feriado fixo: 31 é domingo, 30 é sábado, 29 é dia 29)
        LocalDate dueDate = DarfDueDateCalculator.calculateDueDate(YearMonth.of(2024, 2));
        // Março 2024: dia 31 é domingo, dia 30 é sábado -> dia 29/03/2024 (sexta-feira)
        assertThat(dueDate).isEqualTo(LocalDate.of(2024, 3, 29));

        // Outubro de 2025: próximo mês é Novembro/2025. 30/11/2025 é domingo, 29/11 é sábado, 28/11 é sexta-feira
        LocalDate dueNov = DarfDueDateCalculator.calculateDueDate(YearMonth.of(2025, 10));
        assertThat(dueNov).isEqualTo(LocalDate.of(2025, 11, 28));

        // Abril de 2025: próximo mês é Maio/2025. 31/05/2025 é sábado -> 30/05/2025 é sexta-feira
        LocalDate dueMay = DarfDueDateCalculator.calculateDueDate(YearMonth.of(2025, 4));
        assertThat(dueMay).isEqualTo(LocalDate.of(2025, 5, 30));
    }
}
