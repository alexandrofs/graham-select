package afsdigital.grahamselect.goals.application.usecase;

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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFinancialGoalUseCaseTest {

    @Mock
    private FinancialGoalRepository repository;

    @InjectMocks
    private GetFinancialGoalUseCase useCase;

    @Test
    void execute_existingGoal_shouldReturnGoalDto() {
        // Arrange
        String userId = "user-123";
        FinancialGoal goal = FinancialGoal.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("500000.00"))
                .monthlyContribution(new BigDecimal("2000.00"))
                .estimatedYears(15)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));

        // Act
        Optional<FinancialGoalDto> result = useCase.execute(userId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().goalType()).isEqualTo("PATRIMONY_TARGET");
        assertThat(result.get().targetValue()).isEqualByComparingTo(new BigDecimal("500000.00"));
        verify(repository).findByUserId(userId);
    }

    @Test
    void execute_noGoal_shouldReturnEmpty() {
        // Arrange
        String userId = "user-999";
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        Optional<FinancialGoalDto> result = useCase.execute(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(repository).findByUserId(userId);
    }
}
