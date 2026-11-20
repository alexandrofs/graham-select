package afsdigital.grahamselect.api.tax.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessGuard;
import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;
import afsdigital.grahamselect.common.tax.application.dto.*;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxAssetDeclarationReportUseCase;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxAvailableYearsUseCase;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxMonthlyCapitalGainsUseCase;
import afsdigital.grahamselect.common.tax.domain.entities.DarfStatus;
import afsdigital.grahamselect.common.tax.domain.entities.TaxCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaxDeclarationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetTaxAssetDeclarationReportUseCase getTaxAssetDeclarationReportUseCase;

    @Mock
    private GetTaxAvailableYearsUseCase getTaxAvailableYearsUseCase;

    @Mock
    private GetTaxMonthlyCapitalGainsUseCase getTaxMonthlyCapitalGainsUseCase;

    @Mock
    private PremiumFeatureAccessGuard premiumFeatureAccessGuard;

    @Mock
    private FamilyProfilePort familyProfilePort;

    @InjectMocks
    private TaxDeclarationController controller;

    private final Jwt mockJwt = Jwt.withTokenValue("mock-jwt-token")
            .header("alg", "none")
            .claim("sub", "user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(Jwt.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockJwt;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtResolver)
                .build();
    }

    private TaxAssetDeclarationReportDTO createSampleReport() {
        TaxAssetDeclarationItemDTO item = new TaxAssetDeclarationItemDTO(
                "PETR4",
                "PETROLEO BRASILEIRO S.A. PETROBRAS",
                "33.000.167/0001-01",
                "03",
                "01",
                "Participações societárias",
                "105",
                new BigDecimal("150"),
                new BigDecimal("35.00"),
                new BigDecimal("7000.00"),
                new BigDecimal("5250.00"),
                "150 ações de PETROLEO BRASILEIRO S.A. PETROBRAS (PETR4), CNPJ: 33.000.167/0001-01. Custodiado na corretora XP Investimentos. Custo total de aquisição em 31/12/2025: R$ 5.250,00 (Preço médio de aquisição: R$ 35,00).",
                false
        );

        return new TaxAssetDeclarationReportDTO(
                2025,
                2024,
                new BigDecimal("7000.00"),
                new BigDecimal("5250.00"),
                new BigDecimal("-1750.00"),
                new BigDecimal("-25.00"),
                Map.of("03", new BigDecimal("5250.00")),
                List.of(item)
        );
    }

    @Test
    @DisplayName("GET /api/v1/tax/bens-e-direitos sem year deve retornar 200 OK com ano padrão")
    void shouldReturnReportWithDefaultYear() throws Exception {
        TaxAssetDeclarationReportDTO report = createSampleReport();
        when(getTaxAssetDeclarationReportUseCase.execute("user-123", null, null)).thenReturn(report);

        mockMvc.perform(get("/api/v1/tax/bens-e-direitos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.previousYear").value(2024))
                .andExpect(jsonPath("$.items[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.items[0].groupCode").value("03"))
                .andExpect(jsonPath("$.items[0].itemCode").value("01"));

        verify(getTaxAssetDeclarationReportUseCase).execute("user-123", null, null);
    }

    @Test
    @DisplayName("GET /api/v1/tax/bens-e-direitos com profileId e plano Free deve retornar 403 Forbidden")
    void shouldReturn403WhenProfileIdWithoutPremium() throws Exception {
        String profileId = UUID.randomUUID().toString();
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(false);

        mockMvc.perform(get("/api/v1/tax/bens-e-direitos")
                        .param("profileId", profileId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/tax/bens-e-direitos com profileId não pertencente ao usuário deve retornar 404 Not Found")
    void shouldThrowWhenProfileNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(true);
        when(familyProfilePort.findActiveByIdAndMasterUserId(profileId, "user-123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tax/bens-e-direitos")
                        .param("profileId", profileId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/tax/bens-e-direitos com profileId válido e Premium deve retornar 200 OK")
    void shouldReturnReportForValidFamilyProfile() throws Exception {
        UUID profileId = UUID.randomUUID();
        FamilyProfile profile = FamilyProfile.builder()
                .id(profileId)
                .masterUserId("user-123")
                .name("Filho")
                .cpfDigits("12345678909")
                .active(true)
                .build();

        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(true);
        when(familyProfilePort.findActiveByIdAndMasterUserId(profileId, "user-123")).thenReturn(Optional.of(profile));
        when(getTaxAssetDeclarationReportUseCase.execute("user-123", 2025, profileId.toString()))
                .thenReturn(createSampleReport());

        mockMvc.perform(get("/api/v1/tax/bens-e-direitos")
                        .param("year", "2025")
                        .param("profileId", profileId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025));
    }

    @Test
    @DisplayName("GET /api/v1/tax/bens-e-direitos/export deve retornar CSV com UTF-8 BOM e cabeçalhos corretos")
    void shouldExportCsvSuccessfully() throws Exception {
        TaxAssetDeclarationReportDTO report = createSampleReport();
        when(getTaxAssetDeclarationReportUseCase.execute("user-123", 2025, null)).thenReturn(report);

        byte[] responseBytes = mockMvc.perform(get("/api/v1/tax/bens-e-direitos/export")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"informe_bens_direitos_2025.csv\""))
                .andReturn().getResponse().getContentAsByteArray();

        // Verificar UTF-8 BOM (\uFEFF -> 0xEF, 0xBB, 0xBF)
        assertThat(responseBytes.length).isGreaterThan(3);
        assertThat(responseBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(responseBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(responseBytes[2]).isEqualTo((byte) 0xBF);

        String csvContent = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csvContent).contains("Grupo;Código;Localização;CNPJ;Ticker;Ativo;Quantidade;Preço Médio;Situação 31/12/2024;Situação 31/12/2025;Discriminação");
        assertThat(csvContent).contains("03;01;105;33.000.167/0001-01;PETR4;PETROLEO BRASILEIRO S.A. PETROBRAS;150;35,00;7000,00;5250,00;");
    }

    @Test
    @DisplayName("GET /api/v1/tax/available-years deve retornar 200 OK com lista de anos")
    void shouldReturnAvailableYears() throws Exception {
        when(getTaxAvailableYearsUseCase.execute("user-123", null))
                .thenReturn(new TaxAvailableYearsDTO(List.of(2025, 2024, 2023)));

        mockMvc.perform(get("/api/v1/tax/available-years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[0]").value(2025))
                .andExpect(jsonPath("$.years[1]").value(2024))
                .andExpect(jsonPath("$.years[2]").value(2023));
    }

    private AnnualTaxSummaryDTO createSampleMonthlySummary() {
        MonthlyTradeOperationDTO op = new MonthlyTradeOperationDTO(
                "PETR4",
                java.time.LocalDate.of(2025, 3, 15),
                "VENDA",
                new BigDecimal("100.0000"),
                new BigDecimal("40.00"),
                new BigDecimal("30.0000"),
                new BigDecimal("4000.00"),
                new BigDecimal("1000.00"),
                TaxCategory.SWING_TRADE_EQUITY
        );

        MonthlyTaxCalculationDTO march = new MonthlyTaxCalculationDTO(
                3,
                2025,
                new BigDecimal("4000.00"),
                true,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                DarfStatus.EXEMPT,
                BigDecimal.ZERO,
                null,
                List.of(op)
        );

        return new AnnualTaxSummaryDTO(
                2025,
                List.of(march),
                BigDecimal.ZERO,
                new BigDecimal("4000.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    @Test
    @DisplayName("GET /api/v1/tax/monthly-capital-gains deve retornar 200 OK com resumo anual")
    void shouldReturnMonthlyCapitalGainsSummary() throws Exception {
        AnnualTaxSummaryDTO summary = createSampleMonthlySummary();
        when(getTaxMonthlyCapitalGainsUseCase.execute("user-123", 2025, null)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/tax/monthly-capital-gains")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.totalSalesYear").value(4000.00))
                .andExpect(jsonPath("$.totalExemptProfitYear").value(1000.00))
                .andExpect(jsonPath("$.monthlyCalculations[0].month").value(3))
                .andExpect(jsonPath("$.monthlyCalculations[0].isEquityExempt").value(true))
                .andExpect(jsonPath("$.monthlyCalculations[0].darfStatus").value("EXEMPT"));

        verify(getTaxMonthlyCapitalGainsUseCase).execute("user-123", 2025, null);
    }

    @Test
    @DisplayName("GET /api/v1/tax/monthly-capital-gains com profileId sem plano Premium deve retornar 403 Forbidden")
    void shouldReturn403WhenMonthlyGainsWithProfileIdWithoutPremium() throws Exception {
        String profileId = UUID.randomUUID().toString();
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(false);

        mockMvc.perform(get("/api/v1/tax/monthly-capital-gains")
                        .param("profileId", profileId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/tax/monthly-capital-gains com profileId não encontrado deve retornar 404 Not Found")
    void shouldReturn404WhenMonthlyGainsProfileNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(true);
        when(familyProfilePort.findActiveByIdAndMasterUserId(profileId, "user-123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tax/monthly-capital-gains")
                        .param("profileId", profileId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/tax/monthly-capital-gains/export deve retornar CSV com UTF-8 BOM e seções corretas")
    void shouldExportMonthlyCapitalGainsCsvSuccessfully() throws Exception {
        AnnualTaxSummaryDTO summary = createSampleMonthlySummary();
        when(getTaxMonthlyCapitalGainsUseCase.execute("user-123", 2025, null)).thenReturn(summary);

        byte[] responseBytes = mockMvc.perform(get("/api/v1/tax/monthly-capital-gains/export")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"apuracao_mensal_ganhos_capital_2025.csv\""))
                .andReturn().getResponse().getContentAsByteArray();

        // Verificar UTF-8 BOM (\uFEFF -> 0xEF, 0xBB, 0xBF)
        assertThat(responseBytes.length).isGreaterThan(3);
        assertThat(responseBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(responseBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(responseBytes[2]).isEqualTo((byte) 0xBF);

        String csvContent = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csvContent).contains("Mês;Alienações Ações (R$);Isento Ações;Lucro Isento (R$);");
        assertThat(csvContent).contains("Data;Ticker;Tipo;Quantidade;Preço Venda (R$);Preço Médio (R$);Valor Alienação (R$);Resultado Líquido (R$);Classificação");
        assertThat(csvContent).contains("03/2025;4000,00;Sim;1000,00;");
        assertThat(csvContent).contains("15/03/2025;PETR4;VENDA;100;40,00;30,00;4000,00;1000,00;SWING_TRADE_EQUITY");
    }

}
