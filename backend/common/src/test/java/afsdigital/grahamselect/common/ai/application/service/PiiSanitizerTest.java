package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioPosition;
import afsdigital.grahamselect.common.ai.domain.model.RawPortfolioSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import afsdigital.grahamselect.common.ai.domain.service.TickerAllowlistCatalog;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PiiSanitizerTest {

    private final PiiSanitizer sanitizer = new PiiSanitizer(new TickerAllowlistCatalog());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldStripAllPiiMonetaryValuesAndQuantitiesAndAggregateCanonicalPositions() throws Exception {
        RawPortfolioSnapshot raw = RawPortfolioSnapshot.builder()
                .userId(998877L)
                .ownerName("João da Silva")
                .cpf("123.456.789-00")
                .email("joao.silva@example.com")
                .phone("+55 11 98765-4321")
                .totalNetWorthBrl(new BigDecimal("250000.00"))
                .targetAllocationsPercent(Map.of(
                        MacroClass.ACOES_BR, new BigDecimal("40.00"),
                        MacroClass.RENDA_FIXA, new BigDecimal("30.00"),
                        MacroClass.INTERNACIONAL, new BigDecimal("20.00"),
                        MacroClass.CRIPTO, new BigDecimal("10.00")
                ))
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("B3SA3")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_GROWTH)
                                .quantity(new BigDecimal("100"))
                                .weightPercent(new BigDecimal("20.00"))
                                .marketValueBrl(new BigDecimal("50000.00"))
                                .averagePriceBrl(new BigDecimal("12.50"))
                                .brokerName("XP Investimentos")
                                .accountLabel("Conta Principal")
                                .qualityGrade(9)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4F")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("50"))
                                .weightPercent(new BigDecimal("10.00"))
                                .marketValueBrl(new BigDecimal("25000.00"))
                                .brokerName("Clear")
                                .qualityGrade(8)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("PETR4")
                                .macroClass(MacroClass.ACOES_BR)
                                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                                .quantity(new BigDecimal("50"))
                                .weightPercent(new BigDecimal("10.00"))
                                .marketValueBrl(new BigDecimal("25000.00"))
                                .brokerName("Rico")
                                .qualityGrade(8)
                                .quarantineCorporateEvent(true)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("AAPL")
                                .macroClass(MacroClass.INTERNACIONAL)
                                .subClass(CerradoSubClass.INTERNATIONAL_EQUITY)
                                .quantity(new BigDecimal("10"))
                                .weightPercent(new BigDecimal("20.00"))
                                .qualityGrade(10)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("TESOURO_SELIC_2029")
                                .macroClass(MacroClass.RENDA_FIXA)
                                .subClass(CerradoSubClass.TREASURY_POST_FIXED)
                                .quantity(new BigDecimal("2"))
                                .weightPercent(new BigDecimal("15.00"))
                                .qualityGrade(10)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("CDB Banco Inter CPF 123.456.789-00 João R$ 37.500")
                                .macroClass(MacroClass.RENDA_FIXA)
                                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                                .quantity(new BigDecimal("1"))
                                .weightPercent(new BigDecimal("15.00"))
                                .qualityGrade(7)
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("BTC-BRL")
                                .macroClass(MacroClass.CRIPTO)
                                .subClass(CerradoSubClass.CRYPTO_MAJOR)
                                .quantity(new BigDecimal("0.25"))
                                .weightPercent(new BigDecimal("10.00"))
                                .qualityGrade(8)
                                .build()
                ))
                .build();

        SanitizedPortfolioSnapshot sanitized = sanitizer.sanitize(raw);
        String json = objectMapper.writeValueAsString(sanitized);

        assertThat(json)
                .doesNotContain("João", "123.456.789-00", "joao.silva@example.com", "98765-4321", "998877")
                .doesNotContain("XP Investimentos", "Clear", "Rico", "Conta Principal", "Banco Inter")
                .doesNotContain("250000", "50000", "37.500", "R$", "quantity");

        assertThat(sanitized.positions())
                .extracting(SanitizedAssetPosition::canonicalTicker)
                .containsExactly("AAPL", "B3SA3", "BTC", "PETR4", "RENDA_FIXA_MANUAL_1", "TESOURO_SELIC_2029");

        SanitizedAssetPosition petr4 = sanitized.positions().stream()
                .filter(p -> p.canonicalTicker().equals("PETR4"))
                .findFirst()
                .orElseThrow();
        assertThat(petr4.weightPercent()).isEqualByComparingTo("20.00");
        assertThat(petr4.quarantineCorporateEvent()).isTrue();

        MacroClassAllocationSnapshot acoesBr = sanitized.macroClassAllocations().stream()
                .filter(a -> a.macroClass() == MacroClass.ACOES_BR)
                .findFirst()
                .orElseThrow();
        assertThat(acoesBr.currentPercent()).isEqualByComparingTo("40.00");
        assertThat(acoesBr.targetPercent()).isEqualByComparingTo("40.00");
        assertThat(acoesBr.deviationPercent()).isEqualByComparingTo("0.00");
        assertThat(sanitized.hhiIndex()).isEqualByComparingTo("0.1750");
    }

    @Test
    void shouldAssignDeterministicManualLabelsAndRejectLiteralManualAttempt() {
        RawPortfolioSnapshot raw = RawPortfolioSnapshot.builder()
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("RENDA_FIXA_MANUAL_1")
                                .macroClass(MacroClass.RENDA_FIXA)
                                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                                .quantity(new BigDecimal("10"))
                                .weightPercent(new BigDecimal("30.00"))
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("CDB Banco B")
                                .macroClass(MacroClass.RENDA_FIXA)
                                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                                .quantity(new BigDecimal("5"))
                                .weightPercent(new BigDecimal("40.00"))
                                .build(),
                        RawPortfolioPosition.builder()
                                .rawTicker("Token Privado")
                                .macroClass(MacroClass.CRIPTO)
                                .subClass(CerradoSubClass.OUTROS_MANUAL)
                                .quantity(new BigDecimal("2"))
                                .weightPercent(new BigDecimal("30.00"))
                                .build()
                ))
                .build();

        SanitizedPortfolioSnapshot sanitized = sanitizer.sanitize(raw);
        assertThat(sanitized.positions())
                .extracting(SanitizedAssetPosition::canonicalTicker)
                .containsExactly("CRIPTO_MANUAL_1", "RENDA_FIXA_MANUAL_1", "RENDA_FIXA_MANUAL_2");
    }

    @Test
    void shouldProduceIdenticalManualLabelsWhenInputOrderIsReversed() {
        RawPortfolioPosition manualA = RawPortfolioPosition.builder()
                .rawTicker("CDB Banco Alfa")
                .macroClass(MacroClass.RENDA_FIXA)
                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                .quantity(new BigDecimal("10"))
                .weightPercent(new BigDecimal("40.00"))
                .qualityGrade(7)
                .build();
        RawPortfolioPosition manualB = RawPortfolioPosition.builder()
                .rawTicker("CDB Banco Beta")
                .macroClass(MacroClass.RENDA_FIXA)
                .subClass(CerradoSubClass.FIXED_INCOME_PRIVATE)
                .quantity(new BigDecimal("20"))
                .weightPercent(new BigDecimal("60.00"))
                .qualityGrade(9)
                .build();

        SanitizedPortfolioSnapshot forward = sanitizer.sanitize(
                RawPortfolioSnapshot.builder().positions(List.of(manualA, manualB)).build());
        SanitizedPortfolioSnapshot reversed = sanitizer.sanitize(
                RawPortfolioSnapshot.builder().positions(List.of(manualB, manualA)).build());

        assertThat(reversed).isEqualTo(forward);
    }

    @Test
    void shouldHandleEmptyPortfolioAndNullQualityGradeAndNullSubClass() {
        SanitizedPortfolioSnapshot empty = sanitizer.sanitize(RawPortfolioSnapshot.builder().build());
        assertThat(empty.positions()).isEmpty();
        assertThat(empty.hhiIndex()).isEqualByComparingTo("0.0000");

        RawPortfolioSnapshot withNulls = RawPortfolioSnapshot.builder()
                .positions(List.of(
                        RawPortfolioPosition.builder()
                                .rawTicker("Ativo Sem Classificacao")
                                .macroClass(MacroClass.RENDA_FIXA)
                                .subClass(null)
                                .qualityGrade(null)
                                .quantity(new BigDecimal("1"))
                                .weightPercent(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        SanitizedPortfolioSnapshot sanitized = sanitizer.sanitize(withNulls);
        assertThat(sanitized.positions()).hasSize(1);
        assertThat(sanitized.positions().get(0).subClass()).isEqualTo(CerradoSubClass.OUTROS_MANUAL);
        assertThat(sanitized.positions().get(0).qualityGrade()).isNull();
    }
}
