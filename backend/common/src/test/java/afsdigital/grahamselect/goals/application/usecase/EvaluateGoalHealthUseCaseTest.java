package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.dto.GoalHealthAssessmentDto;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluateGoalHealthUseCaseTest {

    @Mock
    private FinancialGoalRepository financialGoalRepository;

    @Mock
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    private EvaluateGoalHealthUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EvaluateGoalHealthUseCase(
                financialGoalRepository,
                getPortfolioSummaryUseCase,
                new TimeToGoalCalculator()
        );
    }

    @Test
    void shouldEvaluateGoalHealthSuccessfully() {
        String userId = "user-123";
        FinancialGoal goal = FinancialGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("1000000.00"))
                .monthlyContribution(new BigDecimal("2000.00"))
                .estimatedYears(10)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                new BigDecimal("150000.00"),
                new BigDecimal("10.50"),
                new BigDecimal("5000.00"),
                new BigDecimal("800.00")
        );

        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(getPortfolioSummaryUseCase.execute(userId)).thenReturn(summary);

        Optional<GoalHealthAssessmentDto> result = useCase.execute(userId);

        assertTrue(result.isPresent());
        GoalHealthAssessmentDto dto = result.get();
        assertEquals("OFF_TRACK", dto.status());
        assertFalse(dto.isOnTrack());
        assertEquals(120, dto.estimatedTargetMonths());
        assertEquals(136, dto.projectedTotalMonths());
        assertEquals(16, dto.monthsDelay());
        assertTrue(dto.additionalMonthlyContributionNeeded().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(2, dto.actionSuggestions().size());
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoActiveGoal() {
        String userId = "user-without-goal";
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<GoalHealthAssessmentDto> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldLogErrorAndRethrowWhenUnexpectedExceptionOccurs() {
        String userId = "user-error";
        when(financialGoalRepository.findByUserId(userId)).thenThrow(new RuntimeException("DB connection error"));

        assertThrows(RuntimeException.class, () -> useCase.execute(userId));
    }
}
