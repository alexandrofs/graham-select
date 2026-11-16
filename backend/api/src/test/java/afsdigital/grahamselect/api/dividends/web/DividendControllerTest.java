package afsdigital.grahamselect.api.dividends.web;

import afsdigital.grahamselect.common.dividends.application.dto.DividendScheduleDTO;
import afsdigital.grahamselect.common.dividends.application.dto.RelevantFactDTO;
import afsdigital.grahamselect.common.dividends.application.usecase.GetDividendScheduleUseCase;
import afsdigital.grahamselect.common.dividends.application.usecase.GetRelevantFactsUseCase;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DividendControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetDividendScheduleUseCase getDividendScheduleUseCase;

    @Mock
    private GetRelevantFactsUseCase getRelevantFactsUseCase;

    @InjectMocks
    private DividendController controller;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return Jwt.withTokenValue("mock-token")
                        .header("alg", "none")
                        .claim("sub", "user-123")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/dividends/schedule deve retornar lista de proventos com status 200 OK")
    void shouldReturnDividendSchedule() throws Exception {
        DividendScheduleDTO dto = new DividendScheduleDTO(
                UUID.randomUUID(),
                "PETR4",
                CorporateEventType.DIVIDENDO,
                new BigDecimal("1.250000"),
                new BigDecimal("1.250000"),
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 25),
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 5, 20),
                "BRAPI",
                "APPROVED",
                new BigDecimal("100"),
                new BigDecimal("125.00"),
                new BigDecimal("125.00")
        );

        when(getDividendScheduleUseCase.execute(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/dividends/schedule")
                        .param("ticker", "PETR4")
                        .param("onlyMyTickers", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].type").value("DIVIDENDO"))
                .andExpect(jsonPath("$[0].rate").value(1.250000))
                .andExpect(jsonPath("$[0].estimatedTotal").value(125.00));
    }

    @Test
    @DisplayName("GET /api/v1/dividends/facts deve retornar lista de fatos relevantes com status 200 OK")
    void shouldReturnRelevantFacts() throws Exception {
        RelevantFactDTO fact = new RelevantFactDTO(
                UUID.randomUUID(),
                "VALE3",
                "Fato Relevante - Programa de Recompra",
                "Resumo do fato relevante",
                "https://cvm.gov.br/doc/123",
                "FATO_RELEVANTE",
                Instant.now()
        );

        when(getRelevantFactsUseCase.execute(any(), any(), any(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(fact));

        mockMvc.perform(get("/api/v1/dividends/facts")
                        .param("ticker", "VALE3")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("VALE3"))
                .andExpect(jsonPath("$[0].title").value("Fato Relevante - Programa de Recompra"))
                .andExpect(jsonPath("$[0].category").value("FATO_RELEVANTE"));
    }
}
