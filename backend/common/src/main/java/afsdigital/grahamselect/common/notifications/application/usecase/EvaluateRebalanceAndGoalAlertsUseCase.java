package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.goals.application.repository.FinancialGoalRepository;
import afsdigital.grahamselect.goals.domain.entities.FinancialGoal;
import afsdigital.grahamselect.goals.domain.entities.GoalType;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class EvaluateRebalanceAndGoalAlertsUseCase {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("5.00");
    private static final int[] GOAL_MILESTONES = {25, 50, 75, 100};
    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

    private final WhatsappPreferencePort preferencePort;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final AllocationGoalPort allocationGoalPort;
    private final FinancialGoalRepository financialGoalRepository;
    private final SendWhatsappAlertUseCase sendWhatsappAlertUseCase;

    public void execute() {
        List<WhatsappNotificationPreference> optedInUsers = preferencePort.findAllOptedIn();
        if (optedInUsers == null || optedInUsers.isEmpty()) {
            log.info("Nenhum usuário opt-in para avaliação de alertas WhatsApp.");
            return;
        }

        LocalDate cycleDate = LocalDate.now(SAO_PAULO_ZONE);
        log.info("Iniciando avaliação de alertas de rebalanceamento e metas para {} usuários (ciclo={})",
                optedInUsers.size(), cycleDate);

        for (WhatsappNotificationPreference pref : optedInUsers) {
            try {
                if (pref.isNotifyRebalanceTriggers()) {
                    evaluateRebalanceTriggers(pref, cycleDate);
                }
                if (pref.isNotifyGoalMilestones()) {
                    evaluateGoalMilestones(pref);
                }
            } catch (Exception e) {
                log.error("Erro ao avaliar alertas para userId={}: {}", pref.getUserId(), e.getMessage(), e);
            }
        }
    }

    private void evaluateRebalanceTriggers(WhatsappNotificationPreference pref, LocalDate cycleDate) {
        String userId = pref.getUserId();
        List<AllocationGoalDto> goals = allocationGoalPort.findByUserId(userId);
        if (goals == null || goals.isEmpty()) {
            return;
        }

        List<CustodyPositionDTO> positions = getCustodyPositionsUseCase.execute(userId);
        if (positions == null || positions.isEmpty()) {
            return;
        }

        BigDecimal totalEquity = positions.stream()
                .map(CustodyPositionDTO::marketValue)
                .filter(Objects::nonNull)
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalEquity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Map<String, BigDecimal> equityByAssetClass = new HashMap<>();
        Map<String, BigDecimal> equityByTicker = new HashMap<>();
        for (CustodyPositionDTO pos : positions) {
            if (pos.marketValue() == null || pos.marketValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (pos.assetClass() != null) {
                equityByAssetClass.merge(pos.assetClass().trim().toUpperCase(), pos.marketValue(), BigDecimal::add);
            }
            if (pos.ticker() != null) {
                equityByTicker.merge(pos.ticker().trim().toUpperCase(), pos.marketValue(), BigDecimal::add);
            }
        }

        BigDecimal threshold = pref.getRebalanceDriftThresholdPct() != null
                ? pref.getRebalanceDriftThresholdPct()
                : DEFAULT_THRESHOLD;

        for (AllocationGoalDto goal : goals) {
            if (goal.goalType() == null || goal.targetKey() == null || goal.targetPercentage() == null) {
                continue;
            }
            String keyUpper = goal.targetKey().trim().toUpperCase();
            BigDecimal actualValue;
            if ("TICKER".equalsIgnoreCase(goal.goalType())) {
                actualValue = equityByTicker.getOrDefault(keyUpper, BigDecimal.ZERO);
            } else if ("ASSET_CLASS".equalsIgnoreCase(goal.goalType())) {
                actualValue = equityByAssetClass.getOrDefault(keyUpper, BigDecimal.ZERO);
            } else {
                log.warn("Tipo de meta de alocação desconhecido ignorado: userId={}, goalType={}", userId, goal.goalType());
                continue;
            }

            BigDecimal actualPct = actualValue.multiply(ONE_HUNDRED)
                    .divide(totalEquity, 2, RoundingMode.HALF_UP);
            BigDecimal targetPct = goal.targetPercentage().setScale(2, RoundingMode.HALF_UP);
            BigDecimal drift = actualPct.subtract(targetPct).abs();

            if (drift.compareTo(threshold) > 0) {
                String dedupKey = String.format("rebalance:%s:%s:%s:%s",
                        userId, goal.goalType(), keyUpper, cycleDate);
                String message = String.format(
                        "Kerno Rebalanceamento: %s (%s) está com alocação atual de %s%% vs. meta de %s%% (desvio de %s p.p., acima do limiar de %s%%).",
                        keyUpper,
                        goal.goalType(),
                        actualPct.toPlainString(),
                        targetPct.toPlainString(),
                        drift.toPlainString(),
                        threshold.toPlainString()
                );
                sendWhatsappAlertUseCase.sendAlert(
                        pref,
                        WhatsappAlertType.REBALANCE_TRIGGER,
                        dedupKey,
                        message
                );
            }
        }
    }

    private void evaluateGoalMilestones(WhatsappNotificationPreference pref) {
        String userId = pref.getUserId();
        FinancialGoal goal = financialGoalRepository.findByUserId(userId).orElse(null);
        if (goal == null || goal.getId() == null || goal.getTargetValue() == null
                || goal.getTargetValue().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(userId);
        if (summary == null || summary.totalEquity() == null
                || summary.totalEquity().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal progressPct;
        if (goal.getGoalType() == GoalType.PATRIMONY_TARGET) {
            progressPct = summary.totalEquity()
                    .multiply(ONE_HUNDRED)
                    .divide(goal.getTargetValue(), 2, RoundingMode.HALF_UP);
        } else if (goal.getGoalType() == GoalType.MONTHLY_INCOME_TARGET) {
            BigDecimal yieldPct = summary.grossYieldPercentage() != null
                    ? summary.grossYieldPercentage().max(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            BigDecimal estimatedMonthlyIncome = summary.totalEquity()
                    .multiply(yieldPct)
                    .divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)
                    .divide(TWELVE, 6, RoundingMode.HALF_UP);
            progressPct = estimatedMonthlyIncome
                    .multiply(ONE_HUNDRED)
                    .divide(goal.getTargetValue(), 2, RoundingMode.HALF_UP);
        } else {
            return;
        }

        for (int milestone : GOAL_MILESTONES) {
            if (progressPct.compareTo(BigDecimal.valueOf(milestone)) >= 0) {
                String dedupKey = String.format("goal:%s:milestone:%d", goal.getId(), milestone);
                String message = String.format(
                        "Kerno Meta Financeira: Parabéns! Você atingiu o marco de %d%% da sua meta (%s) com progresso atual de %s%%.",
                        milestone,
                        goal.getGoalType().name(),
                        progressPct.toPlainString()
                );
                sendWhatsappAlertUseCase.sendAlert(
                        pref,
                        WhatsappAlertType.GOAL_MILESTONE,
                        dedupKey,
                        message
                );
            }
        }
    }
}
