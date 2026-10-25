package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.dto.GoalProjectionDto;
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
class CalculateGoalProjectionUseCaseTest {

    @Mock
    private FinancialGoalRepository financialGoalRepository;

    @Mock
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    private CalculateGoalProjectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CalculateGoalProjectionUseCase(
                financialGoalRepository,
                getPortfolioSummaryUseCase,
                new TimeToGoalCalculator()
        );
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoFinancialGoal() {
        String userId = "user-no-goal";
        when(getPortfolioSummaryUseCase.execute(userId)).thenReturn(
                new PortfolioSummaryDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<GoalProjectionDto> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCalculateProjectionUsingPortfolioSummaryAndGoalInUtc() {
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

        when(getPortfolioSummaryUseCase.execute(userId)).thenReturn(summary);
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.of(goal));

        Optional<GoalProjectionDto> result = useCase.execute(userId);

        assertTrue(result.isPresent());
        GoalProjectionDto dto = result.get();
        assertEquals(new BigDecimal("1000000.00"), dto.targetValue());
        assertEquals(new BigDecimal("150000.00"), dto.currentEquity());
        assertEquals(new BigDecimal("2000.00"), dto.monthlyContribution());
        assertEquals(new BigDecimal("10.50"), dto.annualYieldPercentage());
        assertEquals(new BigDecimal("15.00"), dto.progressPercentage());
        assertFalse(dto.isAchieved());
        assertFalse(dto.isUnreachable());
        assertNotNull(dto.targetDate());
        assertEquals(ZoneOffset.UTC, dto.targetDate().getOffset());
    }

    @Test
    void shouldRethrowAndLogExceptionWhenErrorOccurs() {
        String userId = "user-err";
        when(getPortfolioSummaryUseCase.execute(userId)).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> useCase.execute(userId));
    }
}
