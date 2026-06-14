package afsdigital.grahamselect.goals.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FinancialGoalDto(
        UUID id,
        String goalType,
        BigDecimal targetValue,
        BigDecimal monthlyContribution,
        int estimatedYears,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
