package afsdigital.grahamselect.goals.domain.services;

import afsdigital.grahamselect.goals.domain.entities.GoalProjection;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de paridade Java x Dart. Os mesmos vetores em
 * {@code goals/time-to-goal-parity-vectors.json} sao executados em
 * {@code frontend/test/features/goals/domain/services/time_to_goal_calculator_parity_test.dart}.
 */
class TimeToGoalCalculatorParityTest {

    @Test
    void shouldMatchSharedParityVectors() throws Exception {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/goals/time-to-goal-parity-vectors.json")) {
            assertTrue(in != null, "vetores de paridade nao encontrados");
            root = new ObjectMapper().readTree(in);
        }
        TimeToGoalCalculator calculator = new TimeToGoalCalculator();
        OffsetDateTime ref = OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

        for (JsonNode v : root.get("vectors")) {
            String name = v.get("name").asText();
            GoalProjection p = calculator.calculate(
                    GoalType.valueOf(v.get("goalType").asText()),
                    new BigDecimal(v.get("targetValue").asText()),
                    new BigDecimal(v.get("currentEquity").asText()),
                    new BigDecimal(v.get("monthlyContribution").asText()),
                    new BigDecimal(v.get("annualYieldPercentage").asText()),
                    ref);
            JsonNode e = v.get("expected");
            assertEquals(e.get("isAchieved").asBoolean(), p.isAchieved(), name + " isAchieved");
            assertEquals(e.get("isUnreachable").asBoolean(), p.isUnreachable(), name + " isUnreachable");
            assertEquals(e.get("totalMonths").asInt(), p.getTotalMonths(), name + " totalMonths");
            assertEquals(e.get("years").asInt(), p.getYears(), name + " years");
            assertEquals(e.get("months").asInt(), p.getMonths(), name + " months");
            assertEquals(e.get("progressPercentage").asDouble(), p.getProgressPercentage().doubleValue(), 0.005, name + " progress");
            assertEquals(e.get("targetValue").asDouble(), p.getTargetValue().doubleValue(), 0.005, name + " targetValue");
        }
    }
}
