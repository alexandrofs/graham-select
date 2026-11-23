package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyDividendAlertsUseCaseTest {

    @Mock
    private WhatsappPreferencePort preferencePort;
    @Mock
    private GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    @Mock
    private SendWhatsappAlertUseCase sendWhatsappAlertUseCase;

    @InjectMocks
    private NotifyDividendAlertsUseCase useCase;

    @Test
    void shouldNotifyUserWithMatchingTickerInCustody() {
        String userId = "user-1";
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .notifyDividends(true)
                .build();

        DividendAnnouncement announcement = DividendAnnouncement.builder()
                .ticker("ITUB4")
                .type(CorporateEventType.JCP)
                .rate(new BigDecimal("1.00"))
                .comDate(LocalDate.of(2026, 11, 25))
                .paymentDate(LocalDate.of(2026, 12, 10))
                .source("BRAPI")
                .build();

        when(preferencePort.findAllOptedIn()).thenReturn(List.of(pref));
        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of(
                new CustodyPositionDTO("ITUB4", new BigDecimal("200"), new BigDecimal("30"), new BigDecimal("35"),
                        new BigDecimal("7000"), BigDecimal.ZERO, "LIVE", Instant.now(), "ACAO")
        ));

        useCase.notifyForNewAnnouncements(List.of(announcement));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendWhatsappAlertUseCase).sendAlert(
                eq(pref),
                eq(WhatsappAlertType.DIVIDEND_ANNOUNCEMENT),
                eq("dividend:user-1:ITUB4:2026-11-25:JCP"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue())
                .startsWith("Kerno Provento:")
                .contains("R$ 170.00")
                .doesNotContain("Graham-Select");
    }
}
