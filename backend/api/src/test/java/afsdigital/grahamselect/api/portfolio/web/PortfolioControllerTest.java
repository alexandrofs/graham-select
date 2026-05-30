package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.MonthlyEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioEvolutionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PortfolioControllerTest extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    @MockBean
    private GetCustodyPositionsUseCase getCustodyPositionsUseCase;

    @MockBean
    private GetPortfolioEvolutionUseCase getPortfolioEvolutionUseCase;

    @Test
    void shouldReturnPortfolioSummary() throws Exception {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                new BigDecimal("1000.00"),
                new BigDecimal("5.5"),
                new BigDecimal("100.00"),
                new BigDecimal("10.00")
        );

        when(getPortfolioSummaryUseCase.execute(anyString())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/portfolios/summary")
                        .with(jwt().jwt(builder -> builder.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEquity").value(1000.00))
                .andExpect(jsonPath("$.grossYieldPercentage").value(5.5))
                .andExpect(jsonPath("$.accumulatedDividends").value(100.00))
                .andExpect(jsonPath("$.monthlyProjection").value(10.00));
    }

    @Test
    void shouldReturnPortfolioEvolution() throws Exception {
        MonthlyEvolutionDTO m1 = new MonthlyEvolutionDTO("2026-04", new BigDecimal("1500.00"), new BigDecimal("50.00"));
        MonthlyEvolutionDTO m2 = new MonthlyEvolutionDTO("2026-05", new BigDecimal("2000.00"), new BigDecimal("100.00"));
        PortfolioEvolutionDTO evolution = new PortfolioEvolutionDTO(java.util.List.of(m1, m2));

        when(getPortfolioEvolutionUseCase.execute("user-1")).thenReturn(evolution);

        mockMvc.perform(get("/api/v1/portfolios/evolution")
                        .with(jwt().jwt(builder -> builder.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyData[0].month").value("2026-04"))
                .andExpect(jsonPath("$.monthlyData[0].totalContributions").value(1500.00))
                .andExpect(jsonPath("$.monthlyData[0].totalDividends").value(50.00))
                .andExpect(jsonPath("$.monthlyData[1].month").value("2026-05"))
                .andExpect(jsonPath("$.monthlyData[1].totalContributions").value(2000.00))
                .andExpect(jsonPath("$.monthlyData[1].totalDividends").value(100.00));
    }
}
