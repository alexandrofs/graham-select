package afsdigital.grahamselect.common.notifications.application.repository;

import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;

public interface WhatsappGatewayPort {
    WhatsappSendResult sendTextMessage(String phoneNumber, String message);
}
