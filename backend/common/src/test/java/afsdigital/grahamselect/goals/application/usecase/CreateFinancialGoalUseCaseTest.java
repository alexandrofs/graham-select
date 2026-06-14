package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.goals.application.dto.CreateFinancialGoalRequest;
import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFinancialGoalUseCaseTest {

    @Mock
    private FinancialGoalRepository repository;

    @InjectMocks
    private CreateFinancialGoalUseCase useCase;

    @Test
    void execute_validPayload_shouldSaveAndReturnDto() {
        // Arrange
        String userId = "user-123";
        CreateFinancialGoalRequest request = new CreateFinancialGoalRequest(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                20
        );

        FinancialGoal savedGoal = FinancialGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("2000.00"))
                .estimatedYears(20)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(repository.save(any(FinancialGoal.class))).thenReturn(savedGoal);

        // Act
        FinancialGoalDto result = useCase.execute(userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.goalType()).isEqualTo("PATRIMONY_TARGET");
        assertThat(result.targetValue()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.monthlyContribution()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.estimatedYears()).isEqualTo(20);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();

        verify(repository).save(any(FinancialGoal.class));
    }

    @Test
    void execute_monthlyIncomeTarget_shouldSaveCorrectGoalType() {
        // Arrange
        String userId = "user-456";
        CreateFinancialGoalRequest request = new CreateFinancialGoalRequest(
                GoalType.MONTHLY_INCOME_TARGET,
                new BigDecimal("10000.00"),
                new BigDecimal("1500.00"),
                15
        );

        FinancialGoal savedGoal = FinancialGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(GoalType.MONTHLY_INCOME_TARGET)
                .targetValue(new BigDecimal("10000.00"))
                .monthlyContribution(new BigDecimal("1500.00"))
                .estimatedYears(15)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(repository.save(any(FinancialGoal.class))).thenReturn(savedGoal);

        // Act
        FinancialGoalDto result = useCase.execute(userId, request);

        // Assert
        assertThat(result.goalType()).isEqualTo("MONTHLY_INCOME_TARGET");
    }

}
