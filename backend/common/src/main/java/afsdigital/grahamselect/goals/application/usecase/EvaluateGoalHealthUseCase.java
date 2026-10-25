package afsdigital.grahamselect.goals.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.dto.GoalHealthAssessmentDto;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalHealthAssessment;
import afsdigital.grahamselect.goals.domain.services.TimeToGoalCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class EvaluateGoalHealthUseCase {

    private final FinancialGoalRepository financialGoalRepository;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final TimeToGoalCalculator timeToGoalCalculator;

    public Optional<GoalHealthAssessmentDto> execute(String userId) {
        log.info("Evaluating financial goal health for user {}", userId);
        try {
            Optional<FinancialGoal> goalOpt = financialGoalRepository.findByUserId(userId);
            if (goalOpt.isEmpty()) {
                return Optional.empty();
            }

            PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(userId);
            BigDecimal currentEquity = (summary != null && summary.totalEquity() != null)
                    ? summary.totalEquity()
                    : BigDecimal.ZERO;
            BigDecimal annualYieldPercentage = (summary != null && summary.grossYieldPercentage() != null)
                    ? summary.grossYieldPercentage()
                    : BigDecimal.ZERO;

            FinancialGoal goal = goalOpt.get();
            GoalHealthAssessment assessment = timeToGoalCalculator.evaluateHealth(
                    goal.getGoalType(),
                    goal.getTargetValue(),
                    goal.getEstimatedYears(),
                    currentEquity,
                    goal.getMonthlyContribution(),
                    annualYieldPercentage,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );

            return Optional.of(new GoalHealthAssessmentDto(
                    assessment.getStatus().name(),
                    assessment.isOnTrack(),
                    assessment.getEstimatedTargetMonths(),
                    assessment.getProjectedTotalMonths(),
                    assessment.getMonthsDelay(),
                    assessment.getAdditionalMonthlyContributionNeeded(),
                    assessment.getRequiredMonthlyContribution(),
                    assessment.getRequiredAnnualYieldPercentage(),
                    assessment.getActionSuggestions()
            ));
        } catch (Exception ex) {
            log.error("Error evaluating financial goal health for user {}", userId, ex);
            throw ex;
        }
    }
}
