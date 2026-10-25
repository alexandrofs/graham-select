package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.dto.GoalProjectionDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalProjection;
import afsdigital.grahamselect.goals.domain.services.TimeToGoalCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CalculateGoalProjectionUseCase {

    private final FinancialGoalRepository financialGoalRepository;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final TimeToGoalCalculator timeToGoalCalculator;

    public Optional<GoalProjectionDto> execute(String userId) {
        PortfolioSummaryDTO summary;
        try {
            summary = getPortfolioSummaryUseCase.execute(userId);
        } catch (Exception ex) {
            log.error("Error calculating time-to-goal projection for user {}", userId, ex);
            throw ex;
        }
        BigDecimal totalEquity = (summary != null && summary.totalEquity() != null)
                ? summary.totalEquity()
                : BigDecimal.ZERO;
        BigDecimal grossYieldPercentage = (summary != null && summary.grossYieldPercentage() != null)
                ? summary.grossYieldPercentage()
                : BigDecimal.ZERO;

        return execute(userId, totalEquity, grossYieldPercentage);
    }

    public Optional<GoalProjectionDto> execute(String userId, BigDecimal currentEquity, BigDecimal grossYieldPercentage) {
        log.info("Calculating time-to-goal projection for user {}", userId);
        try {
            Optional<FinancialGoal> goalOpt = financialGoalRepository.findByUserId(userId);
            if (goalOpt.isEmpty()) {
                return Optional.empty();
            }

            FinancialGoal goal = goalOpt.get();
            BigDecimal safeEquity = currentEquity != null ? currentEquity : BigDecimal.ZERO;
            BigDecimal safeYield = grossYieldPercentage != null ? grossYieldPercentage : BigDecimal.ZERO;

            GoalProjection projection = timeToGoalCalculator.calculate(
                    goal.getGoalType(),
                    goal.getTargetValue(),
                    safeEquity,
                    goal.getMonthlyContribution(),
                    safeYield,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );

            return Optional.of(toDto(projection));
        } catch (Exception ex) {
            log.error("Error calculating time-to-goal projection for user {}", userId, ex);
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
