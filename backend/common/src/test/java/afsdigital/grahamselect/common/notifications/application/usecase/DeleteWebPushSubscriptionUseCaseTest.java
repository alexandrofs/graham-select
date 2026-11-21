package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.exceptions.WebPushSubscriptionNotFoundException;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteWebPushSubscriptionUseCaseTest {

    @Mock
    private WebPushSubscriptionPort subscriptionPort;

    @InjectMocks
    private DeleteWebPushSubscriptionUseCase useCase;

    @Test
    void shouldDeleteSubscriptionOwnedByUser() {
        String userId = "user-123";
        UUID subId = UUID.randomUUID();
        WebPushSubscription sub = WebPushSubscription.builder()
                .id(subId)
                .userId(userId)
                .endpoint("https://push.example.com")
                .active(true)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(subscriptionPort.findById(subId)).thenReturn(Optional.of(sub));

        useCase.execute(userId, subId);

        verify(subscriptionPort).delete(subId);
    }

    @Test
    void shouldThrowNotFoundWhenSubscriptionDoesNotExist() {
        String userId = "user-123";
        UUID subId = UUID.randomUUID();
        when(subscriptionPort.findById(subId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, subId))
                .isInstanceOf(WebPushSubscriptionNotFoundException.class);
    }

    @Test
    void shouldThrowNotFoundWhenSubscriptionBelongsToAnotherUser() {
        String userId = "user-123";
        UUID subId = UUID.randomUUID();
        WebPushSubscription sub = WebPushSubscription.builder()
                .id(subId)
                .userId("other-user")
                .endpoint("https://push.example.com")
                .active(true)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(subscriptionPort.findById(subId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> useCase.execute(userId, subId))
                .isInstanceOf(WebPushSubscriptionNotFoundException.class);
    }
}
