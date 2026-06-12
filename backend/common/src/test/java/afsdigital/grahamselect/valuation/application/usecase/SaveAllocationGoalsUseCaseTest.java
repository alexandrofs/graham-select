package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.AllocationGoalValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SaveAllocationGoalsUseCaseTest {

    @Mock
    private AllocationGoalPort allocationGoalPort;

    @InjectMocks
    private SaveAllocationGoalsUseCase saveAllocationGoalsUseCase;

    @Test
    public void shouldSaveGoalsWhenAssetClassSumIsExactly100() {
        String userId = "user-123";
        List<AllocationGoalDto> goals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("50.00")),
            new AllocationGoalDto("ASSET_CLASS", "FIIS", new BigDecimal("30.00")),
            new AllocationGoalDto("ASSET_CLASS", "RENDA_FIXA", new BigDecimal("20.00")),
            new AllocationGoalDto("TICKER", "PETR4", new BigDecimal("10.00"))
        );

        saveAllocationGoalsUseCase.execute(userId, goals);

        verify(allocationGoalPort).saveAllocationGoals(userId, goals);
    }

    @Test
    public void shouldThrowExceptionWhenAssetClassSumIsLessThan100() {
        String userId = "user-123";
        List<AllocationGoalDto> goals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("40.00")),
            new AllocationGoalDto("ASSET_CLASS", "FIIS", new BigDecimal("30.00"))
        );

        AllocationGoalValidationException exception = assertThrows(
            AllocationGoalValidationException.class,
            () -> saveAllocationGoalsUseCase.execute(userId, goals)
        );

        assertEquals("A soma das metas de classe deve ser exatamente 100%. Soma atual: 70.00%", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenAssetClassSumIsGreaterThan100() {
        String userId = "user-123";
        List<AllocationGoalDto> goals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("60.00")),
            new AllocationGoalDto("ASSET_CLASS", "FIIS", new BigDecimal("50.00"))
        );

        AllocationGoalValidationException exception = assertThrows(
            AllocationGoalValidationException.class,
            () -> saveAllocationGoalsUseCase.execute(userId, goals)
        );

        assertEquals("A soma das metas de classe deve ser exatamente 100%. Soma atual: 110.00%", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenOnlyTickerGoalsProvidedWithoutClassGoals() {
        String userId = "user-123";
        List<AllocationGoalDto> goals = List.of(
            new AllocationGoalDto("TICKER", "PETR4", new BigDecimal("10.00")),
            new AllocationGoalDto("TICKER", "VALE3", new BigDecimal("15.00"))
        );

        AllocationGoalValidationException exception = assertThrows(
            AllocationGoalValidationException.class,
            () -> saveAllocationGoalsUseCase.execute(userId, goals)
        );

        assertEquals("A lista de metas deve conter classes de ativos", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenGoalsListIsNull() {
        String userId = "user-123";
        AllocationGoalValidationException exception = assertThrows(
            AllocationGoalValidationException.class,
            () -> saveAllocationGoalsUseCase.execute(userId, null)
        );
        assertEquals("A lista de metas não pode ser nula", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenGoalHasNegativePercentage() {
        String userId = "user-123";
        List<AllocationGoalDto> goals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("110.00")),
            new AllocationGoalDto("ASSET_CLASS", "FIIS", new BigDecimal("-10.00"))
        );

        AllocationGoalValidationException exception = assertThrows(
            AllocationGoalValidationException.class,
            () -> saveAllocationGoalsUseCase.execute(userId, goals)
        );
        assertEquals("O percentual de alocação não pode ser negativo para FIIS", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenGoalHasDuplicateKey() {
        String userId = "user-123";
        List<AllocationGoalDto> goals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("50.00")),
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("50.00"))
        );

        AllocationGoalValidationException exception = assertThrows(
            AllocationGoalValidationException.class,
            () -> saveAllocationGoalsUseCase.execute(userId, goals)
        );
        assertEquals("Metas duplicadas não são permitidas para ACOES", exception.getMessage());
    }
}
