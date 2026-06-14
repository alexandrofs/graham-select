package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.goals.application.dto.CreateFinancialGoalRequest;
import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.application.service.exceptions.FinancialGoalNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateFinancialGoalUseCaseTest {

    @Mock
    private FinancialGoalRepository repository;

    @InjectMocks
    private UpdateFinancialGoalUseCase useCase;

    @Test
    void execute_existingGoal_shouldUpdateAndReturnDto() {
        // Arrange
        UUID goalId = UUID.randomUUID();
        String userId = "user-123";
        OffsetDateTime originalCreatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(5);

        FinancialGoal existingGoal = FinancialGoal.builder()
                .id(goalId)
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("300000.00"))
                .monthlyContribution(new BigDecimal("1000.00"))
                .estimatedYears(15)
                .createdAt(originalCreatedAt)
                .updatedAt(originalCreatedAt)
                .build();

        CreateFinancialGoalRequest updateRequest = new CreateFinancialGoalRequest(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                20
        );

        FinancialGoal updatedGoal = FinancialGoal.builder()
                .id(goalId)
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("2000.00"))
                .estimatedYears(20)
                .createdAt(originalCreatedAt)
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(repository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(existingGoal));
        when(repository.save(any(FinancialGoal.class))).thenReturn(updatedGoal);

        // Act
        FinancialGoalDto result = useCase.execute(goalId, userId, updateRequest);

        // Assert
        assertThat(result.id()).isEqualTo(goalId);
        assertThat(result.targetValue()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.estimatedYears()).isEqualTo(20);
        assertThat(result.updatedAt()).isAfterOrEqualTo(originalCreatedAt);

        verify(repository).findByIdAndUserId(goalId, userId);
        verify(repository).save(any(FinancialGoal.class));
    }

    @Test
    void execute_goalNotBelongingToUser_shouldThrowFinancialGoalNotFoundException() {
        // Arrange
        UUID goalId = UUID.randomUUID();
        String userId = "user-999";

        when(repository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        CreateFinancialGoalRequest request = new CreateFinancialGoalRequest(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                20
        );

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(goalId, userId, request))
                .isInstanceOf(FinancialGoalNotFoundException.class)
                .hasMessageContaining(goalId.toString());
    }
}
