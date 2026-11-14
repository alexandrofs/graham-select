package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllocationGoalsUseCaseTest {

    @Mock
    private AllocationGoalPort allocationGoalPort;

    @Mock
    private PortfolioTagPort portfolioTagPort;

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

    @Test
    void shouldReturnScopedAllocationGoalsWhenTagOwnedByUser() {
        String userId = "user-123";
        UUID tagId = UUID.randomUUID();
        List<AllocationGoalDto> expectedGoals = List.of(
            new AllocationGoalDto("ASSET_CLASS", "FIIS", new BigDecimal("100.00"))
        );
        when(portfolioTagPort.findByIdAndUserId(tagId, userId))
                .thenReturn(Optional.of(PortfolioTag.builder().id(tagId).userId(userId).name("FIIs").build()));
        when(allocationGoalPort.findByUserId(userId, tagId)).thenReturn(expectedGoals);

        List<AllocationGoalDto> actualGoals = getAllocationGoalsUseCase.execute(userId, tagId);

        assertEquals(expectedGoals, actualGoals);
    }

    @Test
    void shouldThrowTagNotFoundWhenFetchingGoalsForForeignOrUnknownTag() {
        String userId = "user-123";
        UUID foreignTagId = UUID.randomUUID();
        when(portfolioTagPort.findByIdAndUserId(foreignTagId, userId)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class, () -> getAllocationGoalsUseCase.execute(userId, foreignTagId));
    }
}
