package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.UpdateWhatsappPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateWhatsappPreferencesUseCaseTest {

    @Mock
    private WhatsappPreferencePort preferencePort;

    @InjectMocks
    private UpdateWhatsappPreferencesUseCase useCase;

    @Test
    void shouldValidateE164AndClampThresholdAndSetOptedInAt() {
        String userId = "user-123";
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWhatsappPreferenceRequest request = new UpdateWhatsappPreferenceRequest(
                "+5511999998888",
                true,
                true,
                false,
                true,
                new BigDecimal("85.00")
        );

        WhatsappPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.phoneNumber()).isEqualTo("+5511999998888");
        assertThat(result.optedIn()).isTrue();
        assertThat(result.optedInAt()).isNotNull();
        assertThat(result.rebalanceDriftThresholdPct()).isEqualByComparingTo("50.00");
        assertThat(result.notifyDividends()).isTrue();
        assertThat(result.notifyRebalanceTriggers()).isFalse();
        assertThat(result.notifyGoalMilestones()).isTrue();
    }

    @Test
    void shouldClampThresholdBelowMinimumToOne() {
        String userId = "user-123";
        OffsetDateTime existingOptIn = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        WhatsappNotificationPreference existing = WhatsappNotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .phoneNumber("+5511999998888")
                .optedIn(true)
                .optedInAt(existingOptIn)
                .createdAt(existingOptIn)
                .updatedAt(existingOptIn)
                .build();
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWhatsappPreferenceRequest request = new UpdateWhatsappPreferenceRequest(
                "+5511999998888",
                true,
                null,
                null,
                null,
                new BigDecimal("0.10")
        );

        WhatsappPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.rebalanceDriftThresholdPct()).isEqualByComparingTo("1.00");
        assertThat(result.optedInAt()).isEqualTo(existingOptIn);
    }

    @Test
    void shouldThrowIllegalArgumentForInvalidE164Phone() {
        UpdateWhatsappPreferenceRequest request = new UpdateWhatsappPreferenceRequest(
                "11999998888",
                true,
                true,
                true,
                true,
                new BigDecimal("5.00")
        );

        assertThatThrownBy(() -> useCase.execute("user-123", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E.164");
    }

    @Test
    void shouldAllowOptOutWithEmptyPhoneNumberForLgpdErasure() {
        String userId = "user-123";
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWhatsappPreferenceRequest request = new UpdateWhatsappPreferenceRequest(
                "",
                false,
                true,
                true,
                true,
                new BigDecimal("5.00")
        );

        WhatsappPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.optedIn()).isFalse();
        assertThat(result.optedInAt()).isNull();
        assertThat(result.phoneNumber()).isEmpty();
    }
}
