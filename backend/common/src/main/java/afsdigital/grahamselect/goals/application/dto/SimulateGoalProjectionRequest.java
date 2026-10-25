package afsdigital.grahamselect.goals.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SimulateGoalProjectionRequest(
        @NotNull @PositiveOrZero BigDecimal simulatedMonthlyContribution,
        @NotNull @PositiveOrZero BigDecimal simulatedAnnualYieldPercentage
) {}
