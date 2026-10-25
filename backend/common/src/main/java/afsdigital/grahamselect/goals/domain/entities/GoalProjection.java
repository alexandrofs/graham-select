package afsdigital.grahamselect.goals.domain.entities;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Getter
public class GoalProjection {
    private BigDecimal targetValue;
    private BigDecimal currentEquity;
    private BigDecimal monthlyContribution;
    private BigDecimal annualYieldPercentage;
    private int totalMonths;
    private int years;
    private int months;
    private OffsetDateTime targetDate;
    private BigDecimal progressPercentage;
    private boolean isAchieved;
    private boolean isUnreachable;
}
