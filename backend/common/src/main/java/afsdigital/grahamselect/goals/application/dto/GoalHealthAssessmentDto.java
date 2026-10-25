package afsdigital.grahamselect.goals.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalHealthAssessmentDto(
        String status,
        boolean isOnTrack,
        int estimatedTargetMonths,
        int projectedTotalMonths,
        int monthsDelay,
        BigDecimal additionalMonthlyContributionNeeded,
        BigDecimal requiredMonthlyContribution,
        BigDecimal requiredAnnualYieldPercentage,
        List<String> actionSuggestions
) {}
