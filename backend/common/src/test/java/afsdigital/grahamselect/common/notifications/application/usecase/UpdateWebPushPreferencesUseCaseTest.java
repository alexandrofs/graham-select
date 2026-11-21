package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.dto.UpdateWebPushPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
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
class UpdateWebPushPreferencesUseCaseTest {

    @Mock
    private WebPushPreferencePort preferencePort;

    @InjectMocks
    private UpdateWebPushPreferencesUseCase useCase;

    @Test
    void shouldCreateNewPreferenceWithClampedMarginAndOptedInAt() {
        String userId = "user-123";
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWebPushPreferenceRequest request = new UpdateWebPushPreferenceRequest(
                true,
                new BigDecimal("95.00"),
                true
        );

        WebPushPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.optedIn()).isTrue();
        assertThat(result.optedInAt()).isNotNull();
        assertThat(result.minMarginOfSafetyPct()).isEqualByComparingTo("90.00");
        assertThat(result.onlyCustodyTickers()).isTrue();
    }

    @Test
    void shouldClampMarginBelowZeroToZero() {
        String userId = "user-123";
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWebPushPreferenceRequest request = new UpdateWebPushPreferenceRequest(
                true,
                new BigDecimal("-10.00"),
                false
        );

        WebPushPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.minMarginOfSafetyPct()).isEqualByComparingTo("0.00");
        assertThat(result.onlyCustodyTickers()).isFalse();
    }

    @Test
    void shouldPreserveExistingOptedInAtWhenAlreadyOptedIn() {
        String userId = "user-123";
        OffsetDateTime initialOptedInAt = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        WebPushPreference existing = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(true)
                .optedInAt(initialOptedInAt)
                .minMarginOfSafetyPct(new BigDecimal("20.00"))
                .onlyCustodyTickers(true)
                .createdAt(initialOptedInAt)
                .updatedAt(initialOptedInAt)
                .build();

        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWebPushPreferenceRequest request = new UpdateWebPushPreferenceRequest(
                true,
                new BigDecimal("30.00"),
                false
        );

        WebPushPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.optedIn()).isTrue();
        assertThat(result.optedInAt()).isEqualTo(initialOptedInAt);
        assertThat(result.minMarginOfSafetyPct()).isEqualByComparingTo("30.00");
        assertThat(result.onlyCustodyTickers()).isFalse();
    }

    @Test
    void shouldClearOptedInAtWhenRevokingConsent() {
        String userId = "user-123";
        OffsetDateTime initialOptedInAt = OffsetDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        WebPushPreference existing = WebPushPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .optedIn(true)
                .optedInAt(initialOptedInAt)
                .minMarginOfSafetyPct(new BigDecimal("20.00"))
                .onlyCustodyTickers(true)
                .createdAt(initialOptedInAt)
                .updatedAt(initialOptedInAt)
                .build();

        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWebPushPreferenceRequest request = new UpdateWebPushPreferenceRequest(
                false,
                new BigDecimal("25.00"),
                true
        );

        WebPushPreferenceDto result = useCase.execute(userId, request);

        assertThat(result.optedIn()).isFalse();
        assertThat(result.optedInAt()).isNull();
    }

    @Test
    void shouldRejectNullUserIdOrRequest() {
        assertThatThrownBy(() -> useCase.execute(null, new UpdateWebPushPreferenceRequest(true, BigDecimal.TEN, true)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute("user-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
