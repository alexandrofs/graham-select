package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.dto.GoalProjectionDto;
import afsdigital.grahamselect.goals.application.dto.GoalSimulationDto;
import afsdigital.grahamselect.goals.application.dto.SimulateGoalProjectionRequest;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalProjection;
import afsdigital.grahamselect.goals.domain.entities.GoalSimulationResult;
import afsdigital.grahamselect.goals.domain.services.TimeToGoalCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SimulateGoalProjectionUseCase {

    private final FinancialGoalRepository financialGoalRepository;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final TimeToGoalCalculator timeToGoalCalculator;

    public Optional<GoalSimulationDto> execute(String userId, SimulateGoalProjectionRequest request) {
        log.info("Simulating time-to-goal sensitivity for user {}", userId);
        try {
            Optional<FinancialGoal> goalOpt = financialGoalRepository.findByUserId(userId);
            if (goalOpt.isEmpty()) {
                return Optional.empty();
            }

            PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(userId);
            BigDecimal currentEquity = (summary != null && summary.totalEquity() != null)
                    ? summary.totalEquity()
                    : BigDecimal.ZERO;
            BigDecimal baselineYieldPercentage = (summary != null && summary.grossYieldPercentage() != null)
                    ? summary.grossYieldPercentage()
                    : BigDecimal.ZERO;

            FinancialGoal goal = goalOpt.get();
            BigDecimal simulatedContribution = request != null && request.simulatedMonthlyContribution() != null
                    ? request.simulatedMonthlyContribution()
                    : goal.getMonthlyContribution();
            BigDecimal simulatedYield = request != null && request.simulatedAnnualYieldPercentage() != null
                    ? request.simulatedAnnualYieldPercentage()
                    : baselineYieldPercentage;

            GoalSimulationResult result = timeToGoalCalculator.simulate(
                    goal.getGoalType(),
                    goal.getTargetValue(),
                    currentEquity,
                    goal.getMonthlyContribution(),
                    baselineYieldPercentage,
                    simulatedContribution,
                    simulatedYield,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );

            return Optional.of(new GoalSimulationDto(
                    toDto(result.getBaselineProjection()),
                    toDto(result.getSimulatedProjection()),
                    result.getMonthsDifference()
            ));
        } catch (Exception ex) {
            log.error("Error simulating time-to-goal sensitivity for user {}", userId, ex);
            throw ex;
        }
    }

    private GoalProjectionDto toDto(GoalProjection projection) {
        return new GoalProjectionDto(
                projection.getTargetValue(),
                projection.getCurrentEquity(),
                projection.getMonthlyContribution(),
                projection.getAnnualYieldPercentage(),
                projection.getTotalMonths(),
                projection.getYears(),
                projection.getMonths(),
                projection.getTargetDate(),
                projection.getProgressPercentage(),
                projection.isAchieved(),
                projection.isUnreachable()
        );
    }
}
