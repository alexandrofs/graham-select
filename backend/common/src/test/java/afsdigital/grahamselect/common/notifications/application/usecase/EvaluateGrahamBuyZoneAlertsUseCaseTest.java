package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.domain.entities.ValuationCompletedEvent;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluateGrahamBuyZoneAlertsUseCaseTest {

    @Mock
    private WebPushPreferencePort preferencePort;

    @Mock
    private WebPushSubscriptionPort subscriptionPort;

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private GetCustodyPositionsUseCase getCustodyPositionsUseCase;

    @Mock
    private SendWebPushAlertUseCase sendWebPushAlertUseCase;

    @InjectMocks
    private EvaluateGrahamBuyZoneAlertsUseCase useCase;

    private RankedCompany buildCompany(String symbol, BigDecimal margin, LocalDate updateDate, BigDecimal price, BigDecimal intrinsic) {
        return RankedCompany.builder()
                .symbol(symbol)
                .name(symbol + " S.A.")
                .assetClass("ACAO")
                .currentPrice(price)
                .intrinsicValue(intrinsic)
                .marginOfSafety(margin)
                .intrinsicValueUpdatedAt(updateDate)
                .build();
    }

    private WebPushSubscription buildSubscription(String userId) {
        return WebPushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .endpoint("https://push.example.com/sub")
                .endpointHash("hash")
                .p256dhKey("p256")
                .authKey("auth")
                .active(true)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    @Test
    void shouldEvaluateBuyZoneAndSendAlertsMatchingCustodyAndThreshold() {
        String userId = "user-123";
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        WebPushPreference pref = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(true)
                .minMarginOfSafetyPct(new BigDecimal("20.00"))
                .onlyCustodyTickers(true)
                .build();

        WebPushSubscription sub = buildSubscription(userId);

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(pref));
        when(subscriptionPort.findActiveByUserId(userId)).thenReturn(List.of(sub));

        // Company 1: in custody, margin 30% (>= 20%), fresh -> should ALERT
        RankedCompany c1 = buildCompany("PETR4", new BigDecimal("0.30"), todayUtc, new BigDecimal("30.00"), new BigDecimal("42.85"));
        // Company 2: in custody, margin 15% (< 20%) -> should SKIP
        RankedCompany c2 = buildCompany("VALE3", new BigDecimal("0.15"), todayUtc, new BigDecimal("60.00"), new BigDecimal("70.58"));
        // Company 3: NOT in custody, margin 35% -> should SKIP because onlyCustodyTickers=true
        RankedCompany c3 = buildCompany("BBAS3", new BigDecimal("0.35"), todayUtc, new BigDecimal("25.00"), new BigDecimal("38.46"));
        // Company 4: in custody, stale date (10 days old) -> should SKIP
        RankedCompany c4 = buildCompany("ITUB4", new BigDecimal("0.40"), todayUtc.minusDays(10), new BigDecimal("30.00"), new BigDecimal("50.00"));
        // Company 5: in custody, outlier margin 0.95 (> 0.90) -> should SKIP
        RankedCompany c5 = buildCompany("BBDC4", new BigDecimal("0.95"), todayUtc, new BigDecimal("10.00"), new BigDecimal("66.67"));

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(c1, c2, c3, c4, c5));

        CustodyPositionDTO pos1 = new CustodyPositionDTO("PETR4", new BigDecimal("100"), new BigDecimal("28.00"), new BigDecimal("30.00"), new BigDecimal("3000.00"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO");
        CustodyPositionDTO pos2 = new CustodyPositionDTO("VALE3", new BigDecimal("50"), new BigDecimal("55.00"), new BigDecimal("60.00"), new BigDecimal("3000.00"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO");
        CustodyPositionDTO pos4 = new CustodyPositionDTO("ITUB4", new BigDecimal("200"), new BigDecimal("25.00"), new BigDecimal("30.00"), new BigDecimal("6000.00"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO");
        CustodyPositionDTO pos5 = new CustodyPositionDTO("BBDC4", new BigDecimal("150"), new BigDecimal("12.00"), new BigDecimal("10.00"), new BigDecimal("1500.00"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO");

        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of(pos1, pos2, pos4, pos5));

        useCase.execute();

        verify(sendWebPushAlertUseCase).sendAlert(
                eq(userId), eq(sub), eq("PETR4"), eq(WebPushAlertType.GRAHAM_BUY_ZONE),
                contains("graham-buy-zone:" + userId + ":PETR4:"),
                contains("PETR4"),
                contains("30.00%"),
                eq("/graham-recommendations")
        );

        verify(sendWebPushAlertUseCase, never()).sendAlert(any(), any(), eq("VALE3"), any(), any(), any(), any(), any());
        verify(sendWebPushAlertUseCase, never()).sendAlert(any(), any(), eq("BBAS3"), any(), any(), any(), any(), any());
        verify(sendWebPushAlertUseCase, never()).sendAlert(any(), any(), eq("ITUB4"), any(), any(), any(), any(), any());
        verify(sendWebPushAlertUseCase, never()).sendAlert(any(), any(), eq("BBDC4"), any(), any(), any(), any(), any());
    }

    @Test
    void shouldAlertNonCustodyTickersWhenOnlyCustodyIsFalse() {
        String userId = "user-123";
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        WebPushPreference pref = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(true)
                .minMarginOfSafetyPct(new BigDecimal("20.00"))
                .onlyCustodyTickers(false)
                .build();

        WebPushSubscription sub = buildSubscription(userId);

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(pref));
        when(subscriptionPort.findActiveByUserId(userId)).thenReturn(List.of(sub));

        RankedCompany c1 = buildCompany("BBAS3", new BigDecimal("0.25"), todayUtc, new BigDecimal("25.00"), new BigDecimal("33.33"));
        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(c1));

        useCase.execute();

        verify(sendWebPushAlertUseCase).sendAlert(
                eq(userId), eq(sub), eq("BBAS3"), eq(WebPushAlertType.GRAHAM_BUY_ZONE),
                contains("graham-buy-zone:" + userId + ":BBAS3:"),
                contains("BBAS3"),
                contains("25.00%"),
                eq("/graham-recommendations")
        );
    }

    @Test
    void shouldDispatchAlertsConcurrentlyAndIsolateUserFailures() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        WebPushPreference prefFail = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId("user-fail")
                .optedIn(true)
                .minMarginOfSafetyPct(new BigDecimal("20.00"))
                .onlyCustodyTickers(false)
                .build();

        WebPushPreference prefOk = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId("user-ok")
                .optedIn(true)
                .minMarginOfSafetyPct(new BigDecimal("20.00"))
                .onlyCustodyTickers(false)
                .build();

        WebPushSubscription subOk = buildSubscription("user-ok");

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(prefFail, prefOk));
        when(subscriptionPort.findActiveByUserId("user-fail")).thenThrow(new RuntimeException("DB error"));
        when(subscriptionPort.findActiveByUserId("user-ok")).thenReturn(List.of(subOk));

        RankedCompany c1 = buildCompany("ITUB4", new BigDecimal("0.30"), todayUtc, new BigDecimal("28.00"), new BigDecimal("40.00"));
        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(c1));

        useCase.execute();

        verify(sendWebPushAlertUseCase).sendAlert(
                eq("user-ok"), eq(subOk), eq("ITUB4"), eq(WebPushAlertType.GRAHAM_BUY_ZONE),
                contains("graham-buy-zone:user-ok:ITUB4:"),
                contains("ITUB4"),
                contains("30.00%"),
                eq("/graham-recommendations")
        );
    }
}
