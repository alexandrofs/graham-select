package afsdigital.grahamselect.goals.domain.services;

import afsdigital.grahamselect.goals.domain.entities.GoalHealthAssessment;
import afsdigital.grahamselect.goals.domain.entities.GoalHealthStatus;
import afsdigital.grahamselect.goals.domain.entities.GoalProjection;
import afsdigital.grahamselect.goals.domain.entities.GoalSimulationResult;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeToGoalCalculatorTest {

    private TimeToGoalCalculator calculator;
    private OffsetDateTime referenceDate;

    @BeforeEach
    void setUp() {
        calculator = new TimeToGoalCalculator();
        referenceDate = OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void shouldCalculateTimeToGoalWithPositiveYieldAndContributions() {
        // FV = 1,000,000, PV = 150,000, PMT = 2,000, annualYield = 10.50%
        GoalProjection projection = calculator.calculate(
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                referenceDate
        );

        assertFalse(projection.isAchieved());
        assertFalse(projection.isUnreachable());
        assertEquals(new BigDecimal("1000000.00"), projection.getTargetValue());
        assertEquals(new BigDecimal("150000.00"), projection.getCurrentEquity());
        assertEquals(new BigDecimal("15.00"), projection.getProgressPercentage());
        assertTrue(projection.getTotalMonths() > 0);
        assertEquals(projection.getTotalMonths() / 12, projection.getYears());
        assertEquals(projection.getTotalMonths() % 12, projection.getMonths());
        assertEquals(referenceDate.plusMonths(projection.getTotalMonths()), projection.getTargetDate());
    }

    @Test
    void shouldConvertMonthlyIncomeTargetWithPositiveYield() {
        // Target monthly income = 5,000, annualYield = 10% (0.10) -> FV = (5000 * 12) / 0.10 = 600,000.00
        GoalProjection projection = calculator.calculate(
                GoalType.MONTHLY_INCOME_TARGET,
                new BigDecimal("5000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.00"),
                referenceDate
        );

        assertEquals(new BigDecimal("600000.00"), projection.getTargetValue());
        assertEquals(new BigDecimal("10.00"), projection.getProgressPercentage());
        assertFalse(projection.isAchieved());
        assertFalse(projection.isUnreachable());
        assertTrue(projection.getTotalMonths() > 0);
    }

    @Test
    void shouldConvertMonthlyIncomeTargetWithZeroYieldUsingDefaultSixPercentRate() {
        // Target monthly income = 3,000, annualYield = 0% -> uses 6% (0.06) -> FV = (3000 * 12) / 0.06 = 600,000.00
        GoalProjection projection = calculator.calculate(
                GoalType.MONTHLY_INCOME_TARGET,
                new BigDecimal("3000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                referenceDate
        );

        assertEquals(new BigDecimal("600000.00"), projection.getTargetValue());
        assertEquals(new BigDecimal("50.00"), projection.getProgressPercentage());
        // Linear: (600,000 - 300,000) / 5,000 = 60 months (5 years, 0 months)
        assertEquals(60, projection.getTotalMonths());
        assertEquals(5, projection.getYears());
        assertEquals(0, projection.getMonths());
        assertFalse(projection.isAchieved());
        assertFalse(projection.isUnreachable());
    }

    @Test
    void shouldReturnAchievedWhenCurrentEquityEqualsOrExceedsTarget() {
        GoalProjection projection = calculator.calculate(
                new BigDecimal("500000.00"),
                new BigDecimal("550000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("8.00"),
                referenceDate
        );

        assertTrue(projection.isAchieved());
        assertFalse(projection.isUnreachable());
        assertEquals(0, projection.getTotalMonths());
        assertEquals(0, projection.getYears());
        assertEquals(0, projection.getMonths());
        assertEquals(new BigDecimal("100.00"), projection.getProgressPercentage());
        assertEquals(referenceDate, projection.getTargetDate());
    }

    @Test
    void shouldCalculateLinearMonthsWhenYieldIsZeroAndContributionIsPositive() {
        // FV = 100,000, PV = 25,000, PMT = 2,000, r = 0 -> (75,000 / 2,000) = 37.5 -> ceil = 38 months (3 years, 2 months)
        GoalProjection projection = calculator.calculate(
                new BigDecimal("100000.00"),
                new BigDecimal("25000.00"),
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                referenceDate
        );

        assertFalse(projection.isAchieved());
        assertFalse(projection.isUnreachable());
        assertEquals(38, projection.getTotalMonths());
        assertEquals(3, projection.getYears());
        assertEquals(2, projection.getMonths());
        assertEquals(new BigDecimal("25.00"), projection.getProgressPercentage());
        assertNotNull(projection.getTargetDate());
    }

    @Test
    void shouldReturnUnreachableWhenYieldAndContributionAreZeroAndEquityBelowTarget() {
        GoalProjection projection = calculator.calculate(
                new BigDecimal("500000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                referenceDate
        );

        assertFalse(projection.isAchieved());
        assertTrue(projection.isUnreachable());
        assertEquals(-1, projection.getTotalMonths());
        assertEquals(-1, projection.getYears());
        assertEquals(-1, projection.getMonths());
        assertNull(projection.getTargetDate());
        assertEquals(new BigDecimal("20.00"), projection.getProgressPercentage());
    }

    @Test
    void shouldSimulatePositiveMonthsDifferenceWhenContributionOrYieldIncreases() {
        GoalSimulationResult simulation = calculator.simulate(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                new BigDecimal("3500.00"),
                new BigDecimal("12.00"),
                referenceDate
        );

        assertNotNull(simulation);
        assertFalse(simulation.getBaselineProjection().isUnreachable());
        assertFalse(simulation.getSimulatedProjection().isUnreachable());
        assertTrue(simulation.getMonthsDifference() > 0);
        assertEquals(
                simulation.getBaselineProjection().getTotalMonths() - simulation.getSimulatedProjection().getTotalMonths(),
                simulation.getMonthsDifference()
        );
    }

    @Test
    void shouldSimulateNegativeMonthsDifferenceWhenContributionOrYieldDecreases() {
        GoalSimulationResult simulation = calculator.simulate(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("12.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                referenceDate
        );

        assertNotNull(simulation);
        assertTrue(simulation.getMonthsDifference() < 0);
        assertEquals(
                simulation.getBaselineProjection().getTotalMonths() - simulation.getSimulatedProjection().getTotalMonths(),
                simulation.getMonthsDifference()
        );
    }

    @Test
    void shouldSimulateKeepingFixedTargetValueForMonthlyIncomeTargetAndHandleUnreachable() {
        // Monthly income target: 5000 / month at 10% a.a. -> baseline FV = 600,000.00
        // Simulated yield increases to 15% a.a. -> simulated FV MUST remain 600,000.00
        GoalSimulationResult simulation = calculator.simulate(
                GoalType.MONTHLY_INCOME_TARGET,
                new BigDecimal("5000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("15.00"),
                referenceDate
        );

        assertEquals(new BigDecimal("600000.00"), simulation.getBaselineProjection().getTargetValue());
        assertEquals(new BigDecimal("600000.00"), simulation.getSimulatedProjection().getTargetValue());
        assertTrue(simulation.getMonthsDifference() > 0);

        // Unreachable case -> monthsDifference == 0
        GoalSimulationResult unreachableSim = calculator.simulate(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("500000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                referenceDate
        );

        assertTrue(unreachableSim.getSimulatedProjection().isUnreachable());
        assertEquals(0, unreachableSim.getMonthsDifference());
    }

    @Test
    void shouldEvaluateHealthAsOffTrackWhenProjectionExceedsEstimatedYears() {
        // FV = 1,000,000, PV = 150,000, PMT = 2,000, annualYield = 10.50%, estimatedYears = 10 (120 months)
        // Projected months is 136 > 120 -> OFF_TRACK
        GoalHealthAssessment assessment = calculator.evaluateHealth(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("1000000.00"),
                10,
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                referenceDate
        );

        assertEquals(GoalHealthStatus.OFF_TRACK, assessment.getStatus());
        assertFalse(assessment.isOnTrack());
        assertEquals(120, assessment.getEstimatedTargetMonths());
        assertEquals(136, assessment.getProjectedTotalMonths());
        assertEquals(16, assessment.getMonthsDelay());
        assertTrue(assessment.getAdditionalMonthlyContributionNeeded().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(
                new BigDecimal("2000.00").add(assessment.getAdditionalMonthlyContributionNeeded()),
                assessment.getRequiredMonthlyContribution()
        );
        assertTrue(assessment.getRequiredAnnualYieldPercentage().compareTo(new BigDecimal("10.50")) > 0);
        assertEquals(2, assessment.getActionSuggestions().size());
        assertTrue(assessment.getActionSuggestions().get(0).startsWith("Aumente seu aporte em R$ "));
        assertTrue(assessment.getActionSuggestions().get(0).endsWith(" para voltar ao trilho"));
        assertTrue(assessment.getActionSuggestions().get(1).startsWith("Revise seu yield alvo para "));

        // Verify that applying requiredMonthlyContribution puts the goal back on track (<= 120 months)
        GoalProjection adjustedProjection = calculator.calculate(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                assessment.getRequiredMonthlyContribution(),
                new BigDecimal("10.50"),
                referenceDate
        );
        assertFalse(adjustedProjection.isUnreachable());
        assertTrue(adjustedProjection.getTotalMonths() <= 120);

        // Verify that applying requiredAnnualYieldPercentage puts the goal back on track (<= 120 months)
        GoalProjection adjustedYieldProjection = calculator.calculate(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("1000000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                assessment.getRequiredAnnualYieldPercentage(),
                referenceDate
        );
        assertFalse(adjustedYieldProjection.isUnreachable());
        assertTrue(adjustedYieldProjection.getTotalMonths() <= 120);
    }

    @Test
    void shouldEvaluateHealthAsOnTrackWhenProjectionIsWithinEstimatedYears() {
        GoalHealthAssessment assessment = calculator.evaluateHealth(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("1000000.00"),
                15,
                new BigDecimal("150000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.50"),
                referenceDate
        );

        assertEquals(GoalHealthStatus.ON_TRACK, assessment.getStatus());
        assertTrue(assessment.isOnTrack());
        assertEquals(180, assessment.getEstimatedTargetMonths());
        assertEquals(136, assessment.getProjectedTotalMonths());
        assertEquals(0, assessment.getMonthsDelay());
        assertEquals(new BigDecimal("0.00"), assessment.getAdditionalMonthlyContributionNeeded());
        assertEquals(new BigDecimal("2000.00"), assessment.getRequiredMonthlyContribution());
        assertEquals(new BigDecimal("10.50"), assessment.getRequiredAnnualYieldPercentage());
        assertEquals(
                java.util.List.of("Mantenha a consistência dos seus aportes mensais para seguir no trilho."),
                assessment.getActionSuggestions()
        );
    }

    @Test
    void shouldEvaluateHealthAsAchievedWhenEquityMeetsOrExceedsTarget() {
        GoalHealthAssessment assessment = calculator.evaluateHealth(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("500000.00"),
                10,
                new BigDecimal("550000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10.00"),
                referenceDate
        );

        assertEquals(GoalHealthStatus.ACHIEVED, assessment.getStatus());
        assertTrue(assessment.isOnTrack());
        assertEquals(120, assessment.getEstimatedTargetMonths());
        assertEquals(0, assessment.getProjectedTotalMonths());
        assertEquals(0, assessment.getMonthsDelay());
        assertEquals(new BigDecimal("0.00"), assessment.getAdditionalMonthlyContributionNeeded());
        assertEquals(
                java.util.List.of("Parabéns! Sua meta financeira já foi atingida."),
                assessment.getActionSuggestions()
        );
    }

    @Test
    void shouldEvaluateHealthAsOffTrackWhenGoalIsUnreachable() {
        GoalHealthAssessment assessment = calculator.evaluateHealth(
                GoalType.PATRIMONY_TARGET,
                new BigDecimal("240000.00"),
                10,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                referenceDate
        );

        assertEquals(GoalHealthStatus.OFF_TRACK, assessment.getStatus());
        assertFalse(assessment.isOnTrack());
        assertEquals(120, assessment.getEstimatedTargetMonths());
        assertEquals(-1, assessment.getProjectedTotalMonths());
        assertEquals(-1, assessment.getMonthsDelay());
        assertEquals(new BigDecimal("2000.00"), assessment.getAdditionalMonthlyContributionNeeded());
        assertEquals(new BigDecimal("2000.00"), assessment.getRequiredMonthlyContribution());
        assertEquals(new BigDecimal("12.00"), assessment.getRequiredAnnualYieldPercentage());
    }
}
