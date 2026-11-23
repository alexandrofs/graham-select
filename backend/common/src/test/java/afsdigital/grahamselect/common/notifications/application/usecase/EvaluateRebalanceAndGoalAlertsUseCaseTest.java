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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class EvaluateRebalanceAndGoalAlertsUseCaseTest {

    @Mock
    private WhatsappPreferencePort preferencePort;
    @Mock
    private GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    @Mock
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    @Mock
    private AllocationGoalPort allocationGoalPort;
    @Mock
    private FinancialGoalRepository financialGoalRepository;
    @Mock
    private SendWhatsappAlertUseCase sendWhatsappAlertUseCase;

    @InjectMocks
    private EvaluateRebalanceAndGoalAlertsUseCase useCase;

    @Test
    void shouldTriggerRebalanceAndGoalMilestonesWhenThresholdsMet() {
        String userId = "user-123";
        UUID goalId = UUID.randomUUID();
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .notifyRebalanceTriggers(true)
                .notifyGoalMilestones(true)
                .rebalanceDriftThresholdPct(new BigDecimal("5.00"))
                .build();

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(pref));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of(
                new AllocationGoalDto("ASSET_CLASS", "ACAO", new BigDecimal("50.00"))
        ));
        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of(
                new CustodyPositionDTO("PETR4", new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("35"),
                        new BigDecimal("70000"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO"),
                new CustodyPositionDTO("HGLG11", new BigDecimal("100"), new BigDecimal("150"), new BigDecimal("150"),
                        new BigDecimal("30000"), BigDecimal.ZERO, "LIVE", Instant.now(), "FII")
        ));

        FinancialGoal goal = FinancialGoal.builder()
                .id(goalId)
                .userId(userId)
                .goalType(GoalType.PATRIMONY_TARGET)
                .targetValue(new BigDecimal("100000.00"))
                .build();
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(getPortfolioSummaryUseCase.execute(userId)).thenReturn(
                new PortfolioSummaryDTO(new BigDecimal("60000.00"), new BigDecimal("8.00"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        useCase.execute();

        ArgumentCaptor<String> rebalanceMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendWhatsappAlertUseCase).sendAlert(
                eq(pref),
                eq(WhatsappAlertType.REBALANCE_TRIGGER),
                contains("rebalance:user-123:ASSET_CLASS:ACAO:"),
                rebalanceMsgCaptor.capture()
        );
        assertThat(rebalanceMsgCaptor.getValue())
                .startsWith("Kerno Rebalanceamento:")
                .contains("70.00%")
                .doesNotContain("Graham-Select");

        ArgumentCaptor<String> goal25MsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendWhatsappAlertUseCase).sendAlert(
                eq(pref),
                eq(WhatsappAlertType.GOAL_MILESTONE),
                eq("goal:" + goalId + ":milestone:25"),
                goal25MsgCaptor.capture()
        );
        assertThat(goal25MsgCaptor.getValue())
                .startsWith("Kerno Meta Financeira:")
                .contains("25%")
                .doesNotContain("Graham-Select");

        verify(sendWhatsappAlertUseCase).sendAlert(
                eq(pref),
                eq(WhatsappAlertType.GOAL_MILESTONE),
                eq("goal:" + goalId + ":milestone:50"),
                contains("Kerno Meta Financeira:")
        );
    }

    @Test
    void shouldGuardAgainstZeroTotalEquity() {
        String userId = "user-zero";
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .notifyRebalanceTriggers(true)
                .notifyGoalMilestones(true)
                .rebalanceDriftThresholdPct(new BigDecimal("5.00"))
                .build();

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(pref));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of(
                new AllocationGoalDto("ASSET_CLASS", "ACAO", new BigDecimal("50.00"))
        ));
        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of());
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.empty());

        useCase.execute();

        verifyNoInteractions(sendWhatsappAlertUseCase);
    }

    @Test
    void shouldIgnoreUnknownGoalTypeAndEvaluateMonthlyIncomeTarget() {
        String userId = "user-income";
        UUID goalId = UUID.randomUUID();
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .notifyRebalanceTriggers(true)
                .notifyGoalMilestones(true)
                .rebalanceDriftThresholdPct(new BigDecimal("5.00"))
                .build();

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(pref));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of(
                new AllocationGoalDto("UNKNOWN_TYPE", "ANY", new BigDecimal("50.00"))
        ));
        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of(
                new CustodyPositionDTO("PETR4", new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("35"),
                        new BigDecimal("120000"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO")
        ));
        FinancialGoal goal = FinancialGoal.builder()
                .id(goalId)
                .userId(userId)
                .goalType(GoalType.MONTHLY_INCOME_TARGET)
                .targetValue(new BigDecimal("1000.00"))
                .build();
        when(financialGoalRepository.findByUserId(userId)).thenReturn(Optional.of(goal));
        // 120000 * 10% / 12 = 1000/month => 100% progress
        when(getPortfolioSummaryUseCase.execute(userId)).thenReturn(
                new PortfolioSummaryDTO(new BigDecimal("120000.00"), new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        useCase.execute();

        verify(sendWhatsappAlertUseCase, org.mockito.Mockito.never())
                .sendAlert(eq(pref), eq(WhatsappAlertType.REBALANCE_TRIGGER), anyString(), anyString());
        ArgumentCaptor<String> goal100Captor = ArgumentCaptor.forClass(String.class);
        verify(sendWhatsappAlertUseCase).sendAlert(
                eq(pref),
                eq(WhatsappAlertType.GOAL_MILESTONE),
                eq("goal:" + goalId + ":milestone:100"),
                goal100Captor.capture()
        );
        assertThat(goal100Captor.getValue())
                .startsWith("Kerno Meta Financeira:")
                .contains("100%")
                .doesNotContain("Graham-Select");
    }
}
