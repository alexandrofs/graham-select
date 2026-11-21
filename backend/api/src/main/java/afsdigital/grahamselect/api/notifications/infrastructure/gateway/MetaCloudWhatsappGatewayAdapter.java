package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.application.repository.WhatsappGatewayPort;
import afsdigital.grahamselect.common.notifications.application.usecase.WhatsappMaskingUtils;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "meta")
public class MetaCloudWhatsappGatewayAdapter implements WhatsappGatewayPort {

    private static final String PROVIDER = "META";

    private final RestClient restClient;
    private final String phoneNumberId;
    private final String accessToken;

    public MetaCloudWhatsappGatewayAdapter(
            @Value("${whatsapp.meta.base-url:https://graph.facebook.com/v21.0}") String baseUrl,
            @Value("${whatsapp.meta.phone-number-id:}") String phoneNumberId,
            @Value("${whatsapp.meta.access-token:}") String accessToken,
            @Value("${whatsapp.meta.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${whatsapp.meta.read-timeout-ms:10000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    @Override
    public WhatsappSendResult sendTextMessage(String phoneNumber, String message) {
        String maskedPhone = WhatsappMaskingUtils.maskPhoneNumber(phoneNumber);
        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank()) {
            String err = "Credenciais Meta Cloud API ausentes (phoneNumberId ou accessToken).";
            log.error("Falha de configuração Meta WhatsApp para phone={}: {}", maskedPhone, err);
            return new WhatsappSendResult(false, PROVIDER, null, err);
        }

        String recipient = phoneNumber != null ? phoneNumber.replaceAll("[^\\d]", "") : "";
        if (recipient.isBlank()) {
            String err = "Número de telefone destinatário inválido ou vazio.";
            log.error("Falha ao enviar mensagem Meta WhatsApp para phone={}: {}", maskedPhone, err);
            return new WhatsappSendResult(false, PROVIDER, null, err);
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", recipient,
                "type", "text",
                "text", Map.of("body", message != null ? message : "")
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String messageId = extractMessageId(response);
            if (messageId == null || messageId.isBlank()) {
                String err = "Meta Cloud API retornou resposta sem ID de mensagem.";
                log.error("Resposta inesperada da Meta Cloud API para phone={}: {}", maskedPhone, err);
                return new WhatsappSendResult(false, PROVIDER, null, err);
            }
            log.info("Mensagem WhatsApp enviada via Meta Cloud API: provider={}, phone={}, providerMessageId={}",
                    PROVIDER, maskedPhone, messageId);
            return new WhatsappSendResult(true, PROVIDER, messageId, null);
        } catch (Exception e) {
            log.error("Erro ao chamar Meta Cloud API para phone={}: {}", maskedPhone, e.getMessage(), e);
            return new WhatsappSendResult(false, PROVIDER, null,
                    "Falha na comunicação com Meta Cloud API (" + e.getClass().getSimpleName() + ")");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object messagesObj = response.get("messages");
        if (messagesObj instanceof List<?> messages && !messages.isEmpty()) {
            Object first = messages.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                Object id = firstMap.get("id");
                return id != null ? id.toString() : null;
            }
        }
        return null;
    }
}
