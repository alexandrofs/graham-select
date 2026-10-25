package afsdigital.grahamselect.goals.domain.entities;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GoalSimulationResult {
    private GoalProjection baselineProjection;
    private GoalProjection simulatedProjection;
    private int monthsDifference;
}
