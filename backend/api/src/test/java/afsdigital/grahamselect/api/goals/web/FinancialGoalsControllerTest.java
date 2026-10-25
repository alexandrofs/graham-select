package afsdigital.grahamselect.api.goals.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.goals.application.dto.CreateFinancialGoalRequest;
import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.dto.GoalProjectionDto;
import afsdigital.grahamselect.goals.application.dto.GoalSimulationDto;
import afsdigital.grahamselect.goals.application.dto.SimulateGoalProjectionRequest;
import afsdigital.grahamselect.goals.application.service.exceptions.FinancialGoalNotFoundException;
import afsdigital.grahamselect.goals.application.usecase.CalculateGoalProjectionUseCase;
import afsdigital.grahamselect.goals.application.usecase.CreateFinancialGoalUseCase;
import afsdigital.grahamselect.goals.application.usecase.GetFinancialGoalUseCase;
import afsdigital.grahamselect.goals.application.usecase.SimulateGoalProjectionUseCase;
import afsdigital.grahamselect.goals.application.usecase.UpdateFinancialGoalUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FinancialGoalsControllerTest extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateFinancialGoalUseCase createUseCase;

    @MockBean
    private UpdateFinancialGoalUseCase updateUseCase;

    @MockBean
    private GetFinancialGoalUseCase getUseCase;

    @MockBean
    private CalculateGoalProjectionUseCase calculateGoalProjectionUseCase;

    @MockBean
    private SimulateGoalProjectionUseCase simulateGoalProjectionUseCase;

    @Test
    void shouldCreateGoalSuccessfully() throws Exception {
        String userId = "user-123";
        FinancialGoalDto dto = new FinancialGoalDto(
                UUID.randomUUID(),
                "PATRIMONY_TARGET",
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                15,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(createUseCase.execute(eq(userId), any(CreateFinancialGoalRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/financial-goals")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"PATRIMONY_TARGET\",\"targetValue\":500000.00,\"monthlyContribution\":2000.00,\"estimatedYears\":15}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.goalType").value("PATRIMONY_TARGET"))
                .andExpect(jsonPath("$.data.targetValue").value(500000.00));
    }

    @Test
    void shouldReturnBadRequestWhenInputIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/financial-goals")
                        .with(jwt().jwt(builder -> builder.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"PATRIMONY_TARGET\",\"targetValue\":-100.00,\"monthlyContribution\":2000.00,\"estimatedYears\":60}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateGoalSuccessfully() throws Exception {
        String userId = "user-123";
        UUID goalId = UUID.randomUUID();
        FinancialGoalDto dto = new FinancialGoalDto(
                goalId,
                "PATRIMONY_TARGET",
                new BigDecimal("600000.00"),
                new BigDecimal("2500.00"),
                20,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(updateUseCase.execute(eq(goalId), eq(userId), any(CreateFinancialGoalRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/financial-goals/" + goalId)
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"PATRIMONY_TARGET\",\"targetValue\":600000.00,\"monthlyContribution\":2500.00,\"estimatedYears\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetValue").value(600000.00))
                .andExpect(jsonPath("$.data.estimatedYears").value(20));
    }

    @Test
    void shouldReturnForbiddenWhenGoalNotFoundOrNotOwned() throws Exception {
        String userId = "user-123";
        UUID goalId = UUID.randomUUID();

        when(updateUseCase.execute(eq(goalId), eq(userId), any(CreateFinancialGoalRequest.class)))
                .thenThrow(new FinancialGoalNotFoundException("Goal not found"));

        mockMvc.perform(put("/api/v1/financial-goals/" + goalId)
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"PATRIMONY_TARGET\",\"targetValue\":600000.00,\"monthlyContribution\":2500.00,\"estimatedYears\":20}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnEmptyListWhenNoGoalExists() throws Exception {
        String userId = "user-123";
        when(getUseCase.execute(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/financial-goals")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturnProjectionWhenGoalExists() throws Exception {
        String userId = "user-123";
        GoalProjectionDto projectionDto = new GoalProjectionDto(
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                142,
                11,
                10,
                OffsetDateTime.of(2038, 4, 16, 15, 0, 0, 0, ZoneOffset.UTC),
                new BigDecimal("15.00"),
                false,
                false
        );

        when(calculateGoalProjectionUseCase.execute(userId)).thenReturn(Optional.of(projectionDto));

        mockMvc.perform(get("/api/v1/financial-goals/projection")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetValue").value(1000000.00))
                .andExpect(jsonPath("$.data.currentEquity").value(150000.00))
                .andExpect(jsonPath("$.data.totalMonths").value(142))
                .andExpect(jsonPath("$.data.years").value(11))
                .andExpect(jsonPath("$.data.months").value(10))
                .andExpect(jsonPath("$.data.progressPercentage").value(15.00))
                .andExpect(jsonPath("$.data.isAchieved").value(false))
                .andExpect(jsonPath("$.data.isUnreachable").value(false));
    }

    @Test
    void shouldReturnNullDataWhenNoGoalExistsForProjection() throws Exception {
        String userId = "user-123";
        when(calculateGoalProjectionUseCase.execute(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/financial-goals/projection")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void shouldSimulateProjectionSuccessfully() throws Exception {
        String userId = "user-123";
        GoalProjectionDto baselineDto = new GoalProjectionDto(
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                142,
                11,
                10,
                OffsetDateTime.of(2038, 8, 25, 10, 0, 0, 0, ZoneOffset.UTC),
                new BigDecimal("15.00"),
                false,
                false
        );
        GoalProjectionDto simulatedDto = new GoalProjectionDto(
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("12.00"),
                106,
                8,
                10,
                OffsetDateTime.of(2035, 8, 25, 10, 0, 0, 0, ZoneOffset.UTC),
                new BigDecimal("15.00"),
                false,
                false
        );
        GoalSimulationDto simulationDto = new GoalSimulationDto(baselineDto, simulatedDto, 36);

        when(simulateGoalProjectionUseCase.execute(eq(userId), any(SimulateGoalProjectionRequest.class)))
                .thenReturn(Optional.of(simulationDto));

        mockMvc.perform(post("/api/v1/financial-goals/projection/simulate")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"simulatedMonthlyContribution\":3500.00,\"simulatedAnnualYieldPercentage\":12.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baselineProjection.totalMonths").value(142))
                .andExpect(jsonPath("$.data.simulatedProjection.totalMonths").value(106))
                .andExpect(jsonPath("$.data.simulatedProjection.monthlyContribution").value(3500.00))
                .andExpect(jsonPath("$.data.simulatedProjection.annualYieldPercentage").value(12.00))
                .andExpect(jsonPath("$.data.monthsDifference").value(36));
    }

    @Test
    void shouldReturnNullDataWhenNoGoalExistsForSimulateProjection() throws Exception {
        String userId = "user-123";
        when(simulateGoalProjectionUseCase.execute(eq(userId), any(SimulateGoalProjectionRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/financial-goals/projection/simulate")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"simulatedMonthlyContribution\":3500.00,\"simulatedAnnualYieldPercentage\":12.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void shouldReturnBadRequestWhenSimulateProjectionInputIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/financial-goals/projection/simulate")
                        .with(jwt().jwt(builder -> builder.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"simulatedMonthlyContribution\":-100.00,\"simulatedAnnualYieldPercentage\":12.00}"))
                .andExpect(status().isBadRequest());
    }
}
