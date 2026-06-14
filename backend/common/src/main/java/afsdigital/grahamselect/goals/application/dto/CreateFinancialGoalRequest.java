package afsdigital.grahamselect.goals.application.dto;

import afsdigital.grahamselect.goals.domain.entities.GoalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateFinancialGoalRequest(
        @NotNull GoalType goalType,
        @NotNull @Positive BigDecimal targetValue,
        @NotNull @Positive BigDecimal monthlyContribution,
        @Min(1) @Max(50) int estimatedYears
) {}
