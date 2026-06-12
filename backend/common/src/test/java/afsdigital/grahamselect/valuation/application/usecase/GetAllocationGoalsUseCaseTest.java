package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllocationGoalsUseCaseTest {

    @Mock
    private AllocationGoalPort allocationGoalPort;

    @InjectMocks
    private GetAllocationGoalsUseCase getAllocationGoalsUseCase;

    @Test
    void shouldReturnAllocationGoalsFromPort() {
        String userId = "user-123";
        List<AllocationGoalDto> expectedGoals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "ACOES", new BigDecimal("50.00")),
            new AllocationGoalDto("ASSET_CLASS", "FIIS", new BigDecimal("50.00"))
        );
        when(allocationGoalPort.findByUserId(userId)).thenReturn(expectedGoals);

        List<AllocationGoalDto> actualGoals = getAllocationGoalsUseCase.execute(userId);

        assertEquals(expectedGoals, actualGoals);
    }
}
