package afsdigital.grahamselect.goals.domain.entities;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Getter
public class FinancialGoal {

    private UUID id;
    private String userId;
    private GoalType goalType;
    private BigDecimal targetValue;
    private BigDecimal monthlyContribution;
    private int estimatedYears;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
