package afsdigital.grahamselect.goals.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalProjectionDto(
        BigDecimal targetValue,
        BigDecimal currentEquity,
        BigDecimal monthlyContribution,
        BigDecimal annualYieldPercentage,
        int totalMonths,
        int years,
        int months,
        OffsetDateTime targetDate,
        BigDecimal progressPercentage,
        boolean isAchieved,
        boolean isUnreachable
) {}
