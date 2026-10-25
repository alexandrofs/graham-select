package afsdigital.grahamselect.goals.domain.entities;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
public class GoalHealthAssessment {
    private GoalHealthStatus status;
    private boolean isOnTrack;
    private int estimatedTargetMonths;
    private int projectedTotalMonths;
    private int monthsDelay;
    private BigDecimal additionalMonthlyContributionNeeded;
    private BigDecimal requiredMonthlyContribution;
    private BigDecimal requiredAnnualYieldPercentage;
    private List<String> actionSuggestions;
}
