package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioPosition;
import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioSnapshot;
import afsdigital.grahamselect.common.ai.domain.service.TickerAllowlistCatalog;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioStateHashCalculatorTest {

    private final PortfolioStateHashCalculator calculator =
            new PortfolioStateHashCalculator(new TickerAllowlistCatalog());

    @Test
    void shouldProduceIdentical64CharSha256RegardlessOfOrderScaleAggregationOrPii() {
        RawPortfolioSnapshot snapshotA = RawPortfolioSnapshot.builder()
                .userId(1L)
                .ownerName("Alice")
                .cpf("111.222.333-44")
                .totalNetWorthBrl(new BigDecimal("100000.00"))
                .targetAllocationsPercent(Map.of(MacroClass.ACOES_BR, new BigDecimal("50.0")))
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("10.0"))
                                .weightPercent(new BigDecimal("50.0"))
                                .qualityGrade(8)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("AAPL")
                                .macroClass(MacroClass.INTERNACIONAL)
                                .subClass(CerradoSubClass.INTERNATIONAL_EQUITY)
                                .quantity(new BigDecimal("5.00"))
                                .weightPercent(new BigDecimal("50.0"))
                                .qualityGrade(9)
                                .build()
                ))
                .build();

        RawPortfolioSnapshot snapshotB = RawPortfolioSnapshot.builder()
                .userId(2L)
                .ownerName("Bob")
                .cpf("999.888.777-66")
                .totalNetWorthBrl(new BigDecimal("999999.00"))
                .targetAllocationsPercent(Map.of(MacroClass.ACOES_BR, new BigDecimal("50.0000")))
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("AAPL")
                                .macroClass(MacroClass.INTERNACIONAL)
                                .subClass(CerradoSubClass.INTERNATIONAL_EQUITY)
                                .quantity(new BigDecimal("5.0000"))
                                .weightPercent(new BigDecimal("50.00"))
                                .qualityGrade(9)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("6.0000"))
                                .weightPercent(new BigDecimal("30.00"))
                                .qualityGrade(8)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4F")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("4.0000"))
                                .weightPercent(new BigDecimal("20.00"))
                                .qualityGrade(8)
                                .build()
                ))
                .build();

        String hashA = calculator.calculate(snapshotA);
        String hashB = calculator.calculate(snapshotB);

        assertThat(hashA).matches("^[0-9a-f]{64}$");
        assertThat(hashA).isEqualTo(hashB);
    }

    @Test
    void shouldInvalidateHashWhenTargetGradeOrQuarantineChanges() {
        RawPortfolioSnapshot base = snapshotWith(new BigDecimal("50.00"), 8, false);
        RawPortfolioSnapshot targetChanged = snapshotWith(new BigDecimal("55.00"), 8, false);
        RawPortfolioSnapshot gradeChanged = snapshotWith(new BigDecimal("50.00"), 7, false);
        RawPortfolioSnapshot quarantineChanged = snapshotWith(new BigDecimal("50.00"), 8, true);

        String baseHash = calculator.calculate(base);
        assertThat(calculator.calculate(targetChanged)).isNotEqualTo(baseHash);
        assertThat(calculator.calculate(gradeChanged)).isNotEqualTo(baseHash);
        assertThat(calculator.calculate(quarantineChanged)).isNotEqualTo(baseHash);

        RawPortfolioSnapshot quantityChanged = RawPortfolioSnapshot.builder()
                .targetAllocationsPercent(Map.of(MacroClass.ACOES_BR, new BigDecimal("50.00")))
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("11.0000"))
                                .weightPercent(new BigDecimal("100.00"))
                                .qualityGrade(8)
                                .quarantineCorporateEvent(false)
                                .build()
                ))
                .build();
        assertThat(calculator.calculate(quantityChanged)).isNotEqualTo(baseHash);
    }

    @Test
    void shouldProduceSameHashForReversedOrderManualPositions() {
        RawPortfolioPosition manualA = RawPortfolioPosition.builder()
                .rawTicker("CDB Banco Alfa")
                .macroClass(MacroClass.RENDA_FIXA)
                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                .quantity(new BigDecimal("10.0"))
                .weightPercent(new BigDecimal("40.00"))
                .qualityGrade(7)
                .build();
        RawPortfolioPosition manualB = RawPortfolioPosition.builder()
                .rawTicker("CDB Banco Beta")
                .macroClass(MacroClass.RENDA_FIXA)
                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                .quantity(new BigDecimal("20.0000"))
                .weightPercent(new BigDecimal("60.00"))
                .qualityGrade(9)
                .build();

        String forwardHash = calculator.calculate(RawPortfolioSnapshot.builder().positions(List.of(manualA, manualB)).build());
        String reversedHash = calculator.calculate(RawPortfolioSnapshot.builder().positions(List.of(manualB, manualA)).build());

        assertThat(reversedHash).isEqualTo(forwardHash);
    }

    private static RawPortfolioSnapshot snapshotWith(
            BigDecimal targetAcoesBr,
            int qualityGrade,
            boolean quarantine
    ) {
        return RawPortfolioSnapshot.builder()
                .targetAllocationsPercent(Map.of(MacroClass.ACOES_BR, targetAcoesBr))
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("10.0000"))
                                .weightPercent(new BigDecimal("100.00"))
                                .qualityGrade(qualityGrade)
                                .quarantineCorporateEvent(quarantine)
                                .build()
                ))
                .build();
    }
}
