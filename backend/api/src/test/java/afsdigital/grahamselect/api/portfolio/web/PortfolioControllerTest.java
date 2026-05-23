package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
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
}
