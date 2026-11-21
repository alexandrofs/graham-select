package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.notifications.application.exceptions.WebPushSubscriptionNotFoundException;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DeleteWebPushSubscriptionUseCase {

    private final WebPushSubscriptionPort subscriptionPort;

    public void execute(String userId, UUID subscriptionId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (subscriptionId == null) {
            throw new IllegalArgumentException("subscriptionId cannot be null");
        }

        log.info("Removendo assinatura Web Push subscriptionId={} para userId={}", subscriptionId, userId);

        WebPushSubscription subscription = subscriptionPort.findById(subscriptionId)
                .orElseThrow(() -> {
                    log.error("Assinatura Web Push não encontrada para exclusão: subscriptionId={}, userId={}", subscriptionId, userId);
                    return new WebPushSubscriptionNotFoundException(
                            "Assinatura Web Push não encontrada: " + subscriptionId
                    );
                });

        if (!userId.equals(subscription.getUserId())) {
            log.error("Tentativa de remover assinatura de outro usuário: userId={}, ownerId={}", userId, subscription.getUserId());
            throw new WebPushSubscriptionNotFoundException("Assinatura Web Push não encontrada: " + subscriptionId);
        }

        subscriptionPort.delete(subscriptionId);
        log.info("Assinatura Web Push removida com sucesso: {}", subscriptionId);
    }
}
