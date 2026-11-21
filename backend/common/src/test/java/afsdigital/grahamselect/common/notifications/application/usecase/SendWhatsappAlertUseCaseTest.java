package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappDeliveryStatus;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationLog;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendWhatsappAlertUseCaseTest {

    @Mock
    private WhatsappNotificationLogPort logPort;

    @Mock
    private WhatsappGatewayPort gatewayPort;

    @InjectMocks
    private SendWhatsappAlertUseCase useCase;

    @Test
    void shouldSkipWhenSentDedupKeyAlreadyExists() {
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .build();

        WhatsappNotificationLog existingSent = WhatsappNotificationLog.builder()
                .id(UUID.randomUUID())
                .dedupKey("goal:1:milestone:50")
                .status(WhatsappDeliveryStatus.SENT)
                .build();
        when(logPort.findByDedupKey("goal:1:milestone:50")).thenReturn(Optional.of(existingSent));

        boolean sent = useCase.sendAlert(pref, WhatsappAlertType.GOAL_MILESTONE, "goal:1:milestone:50", "Msg");

        assertThat(sent).isFalse();
        verify(gatewayPort, never()).sendTextMessage(any(), any());
        verify(logPort, never()).save(any());
    }

    @Test
    void shouldRetryAndReuseIdWhenPreviousAttemptFailed() {
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .build();

        UUID failedId = UUID.randomUUID();
        WhatsappNotificationLog existingFailed = WhatsappNotificationLog.builder()
                .id(failedId)
                .dedupKey("goal:1:milestone:50")
                .status(WhatsappDeliveryStatus.FAILED)
                .build();
        when(logPort.findByDedupKey("goal:1:milestone:50")).thenReturn(Optional.of(existingFailed));
        when(gatewayPort.sendTextMessage("+5511999998888", "Meta 50%"))
                .thenReturn(new WhatsappSendResult(true, "META", "wamid.1", null));
        when(logPort.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean sent = useCase.sendAlert(pref, WhatsappAlertType.GOAL_MILESTONE, "goal:1:milestone:50", "Meta 50%");

        assertThat(sent).isTrue();
        ArgumentCaptor<WhatsappNotificationLog> captor = ArgumentCaptor.forClass(WhatsappNotificationLog.class);
        verify(logPort).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(failedId);
        assertThat(captor.getValue().getStatus()).isEqualTo(WhatsappDeliveryStatus.SENT);
    }

    @Test
    void shouldSendAndLogWhenOptedInAndNotDuplicated() {
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .build();

        when(logPort.findByDedupKey("goal:1:milestone:50")).thenReturn(Optional.empty());
        when(gatewayPort.sendTextMessage("+5511999998888", "Meta 50%"))
                .thenReturn(new WhatsappSendResult(true, "MOCK", "mock-1", null));
        when(logPort.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean sent = useCase.sendAlert(pref, WhatsappAlertType.GOAL_MILESTONE, "goal:1:milestone:50", "Meta 50%");

        assertThat(sent).isTrue();
        ArgumentCaptor<WhatsappNotificationLog> captor = ArgumentCaptor.forClass(WhatsappNotificationLog.class);
        verify(logPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WhatsappDeliveryStatus.SENT);
        assertThat(captor.getValue().getProvider()).isEqualTo("MOCK");
    }

    @Test
    void shouldRecordFailedLogWhenGatewayThrows() {
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .build();

        when(logPort.findByDedupKey("k1")).thenReturn(Optional.empty());
        when(gatewayPort.sendTextMessage("+5511999998888", "Msg")).thenThrow(new RuntimeException("Timeout"));
        when(logPort.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean sent = useCase.sendAlert(pref, WhatsappAlertType.REBALANCE_TRIGGER, "k1", "Msg");

        assertThat(sent).isFalse();
        ArgumentCaptor<WhatsappNotificationLog> captor = ArgumentCaptor.forClass(WhatsappNotificationLog.class);
        verify(logPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WhatsappDeliveryStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("Timeout");
    }
}
