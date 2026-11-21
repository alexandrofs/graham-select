package afsdigital.grahamselect.api.notifications.infrastructure.gateway;

import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappSendResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetaCloudWhatsappGatewayAdapterTest {

    @Test
    void shouldFailFastWhenCredentialsAreMissing() {
        MetaCloudWhatsappGatewayAdapter adapter = new MetaCloudWhatsappGatewayAdapter(
                "https://graph.facebook.com/v21.0", "", "", 5000, 10000);

        WhatsappSendResult result = adapter.sendTextMessage("+5511999998888", "Hello");

        assertThat(result.success()).isFalse();
        assertThat(result.provider()).isEqualTo("META");
        assertThat(result.errorMessage()).contains("Credenciais");
    }

    @Test
    void shouldFailFastWhenPhoneNumberIsBlank() {
        MetaCloudWhatsappGatewayAdapter adapter = new MetaCloudWhatsappGatewayAdapter(
                "https://graph.facebook.com/v21.0", "12345", "token-abc", 5000, 10000);

        WhatsappSendResult result = adapter.sendTextMessage("   ", "Hello");

        assertThat(result.success()).isFalse();
        assertThat(result.provider()).isEqualTo("META");
        assertThat(result.errorMessage()).contains("inválido");
    }
}
