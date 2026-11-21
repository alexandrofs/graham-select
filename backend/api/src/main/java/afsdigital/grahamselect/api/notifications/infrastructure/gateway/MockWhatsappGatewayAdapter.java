package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WhatsappMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "mock", matchIfMissing = true)
public class MockWhatsappGatewayAdapter implements WhatsappGatewayPort {

    private static final String PROVIDER = "MOCK";

    @Override
    public WhatsappSendResult sendTextMessage(String phoneNumber, String message) {
        String providerMessageId = "mock-wamid-" + UUID.randomUUID();
        log.info("Envio WhatsApp simulado: provider={}, phone={}, providerMessageId={}, preview={}",
                PROVIDER,
                WhatsappMaskingUtils.maskPhoneNumber(phoneNumber),
                providerMessageId,
                WhatsappMaskingUtils.truncate(message, 80));
        return new WhatsappSendResult(true, PROVIDER, providerMessageId, null);
    }
}
