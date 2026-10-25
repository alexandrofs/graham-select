package afsdigital.grahamselect.goals.domain.services;

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
}
