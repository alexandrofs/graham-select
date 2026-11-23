package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.WhatsappTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappNotificationLogPort;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendTestWhatsappAlertUseCaseTest {

    @Mock
    private WhatsappPreferencePort preferencePort;
    @Mock
    private WhatsappNotificationLogPort logPort;
    @Mock
    private WhatsappGatewayPort gatewayPort;

    @InjectMocks
    private SendTestWhatsappAlertUseCase useCase;

    @Test
    void shouldThrowWhenUserNotOptedIn() {
        when(preferencePort.findByUserId("u1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("u1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentimento");
    }

    @Test
    void shouldSendTestAlertAndPersistLog() {
        WhatsappNotificationPreference pref = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId("u1")
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .build();
        when(preferencePort.findByUserId("u1")).thenReturn(Optional.of(pref));
        when(gatewayPort.sendTextMessage(any(), any()))
                .thenReturn(new WhatsappSendResult(true, "MOCK", "mock-123", null));
        when(logPort.save(any())).thenAnswer(i -> i.getArgument(0));

        WhatsappTestAlertResultDto result = useCase.execute("u1");

        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.provider()).isEqualTo("MOCK");
        assertThat(result.providerMessageId()).isEqualTo("mock-123");
        assertThat(result.messagePreview())
                .startsWith("Kerno:")
                .doesNotContain("Graham-Select");
        verify(gatewayPort).sendTextMessage(eq("+5511999998888"), contains("Kerno:"));
    }
}
