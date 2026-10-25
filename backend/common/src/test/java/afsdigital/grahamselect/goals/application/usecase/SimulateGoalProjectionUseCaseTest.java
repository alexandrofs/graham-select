package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.dto.GoalSimulationDto;
import afsdigital.grahamselect.goals.application.dto.SimulateGoalProjectionRequest;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import afsdigital.grahamselect.goals.domain.services.TimeToGoalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulateGoalProjectionUseCaseTest {

    @Mock
    private FinancialGoalRepository financialGoalRepository;

    @Mock
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    private SimulateGoalProjectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SimulateGoalProjectionUseCase(
                financialGoalRepository,
                getPortfolioSummaryUseCase,
                new TimeToGoalCalculator()
        );
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoFinancialGoal() {
        String userId = "user-no-goal";
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.empty());

        SimulateGoalProjectionRequest request = new SimulateGoalProjectionRequest(
                new BigDecimal("3500.00"),
                new BigDecimal("12.00")
        );

        Optional<GoalSimulationDto> result = useCase.execute(userId, request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSimulateProjectionSuccessfullyInUtc() {
        String userId = "user-123";
        FinancialGoal goal = FinancialGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("1000000.00"))
                .monthlyContribution(new BigDecimal("2000.00"))
                .estimatedYears(15)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                new BigDecimal("150000.00"),
                new BigDecimal("10.50"),
                new BigDecimal("5000.00"),
                new BigDecimal("500.00")
        );

        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(getPortfolioSummaryUseCase.execute(userId)).thenReturn(summary);

        SimulateGoalProjectionRequest request = new SimulateGoalProjectionRequest(
                new BigDecimal("3500.00"),
                new BigDecimal("12.00")
        );

        Optional<GoalSimulationDto> result = useCase.execute(userId, request);

        assertTrue(result.isPresent());
        GoalSimulationDto dto = result.get();
        assertNotNull(dto.baselineProjection());
        assertNotNull(dto.simulatedProjection());
        assertEquals(new BigDecimal("2000.00"), dto.baselineProjection().monthlyContribution());
        assertEquals(new BigDecimal("10.50"), dto.baselineProjection().annualYieldPercentage());
        assertEquals(new BigDecimal("3500.00"), dto.simulatedProjection().monthlyContribution());
        assertEquals(new BigDecimal("12.00"), dto.simulatedProjection().annualYieldPercentage());
        assertTrue(dto.monthsDifference() > 0);
        assertFalse(dto.simulatedProjection().isUnreachable());
        assertEquals(ZoneOffset.UTC, dto.simulatedProjection().targetDate().getOffset());
    }

    @Test
    void shouldRethrowAndLogExceptionWhenUnexpectedErrorOccurs() {
        String userId = "user-err";
        when(financialGoalRepository.findByUserId(userId)).thenThrow(new RuntimeException("Database connection failed"));

        SimulateGoalProjectionRequest request = new SimulateGoalProjectionRequest(
                new BigDecimal("3500.00"),
                new BigDecimal("12.00")
        );

        assertThrows(RuntimeException.class, () -> useCase.execute(userId, request));
    }
}
