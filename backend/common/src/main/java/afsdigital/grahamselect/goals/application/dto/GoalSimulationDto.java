package afsdigital.grahamselect.goals.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalSimulationDto(
        GoalProjectionDto baselineProjection,
        GoalProjectionDto simulatedProjection,
        int monthsDifference
) {}
