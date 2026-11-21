package afsdigital.grahamselect.api.notifications.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.notifications.application.dto.RegisterWebPushSubscriptionRequest;
import afsdigital.grahamselect.common.notifications.application.dto.UpdateWebPushPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushNotificationLogDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushSubscriptionDto;
import afsdigital.grahamselect.common.notifications.application.dto.WebPushTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.exceptions.WebPushSubscriptionNotFoundException;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushGatewayPort;
import afsdigital.grahamselect.common.notifications.application.usecase.DeleteWebPushSubscriptionUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWebPushNotificationHistoryUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWebPushPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.ListWebPushSubscriptionsUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.RegisterWebPushSubscriptionUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendTestWebPushAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.UpdateWebPushPreferencesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WebPushNotificationsControllerTest extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebPushGatewayPort gatewayPort;

    @MockBean
    private GetWebPushPreferencesUseCase getPreferencesUseCase;

    @MockBean
    private UpdateWebPushPreferencesUseCase updatePreferencesUseCase;

    @MockBean
    private RegisterWebPushSubscriptionUseCase registerSubscriptionUseCase;

    @MockBean
    private ListWebPushSubscriptionsUseCase listSubscriptionsUseCase;

    @MockBean
    private DeleteWebPushSubscriptionUseCase deleteSubscriptionUseCase;

    @MockBean
    private SendTestWebPushAlertUseCase sendTestAlertUseCase;

    @MockBean
    private GetWebPushNotificationHistoryUseCase getHistoryUseCase;

    @Test
    void shouldReturnVapidPublicKey() throws Exception {
        String userId = "user-jwt-1";
        when(gatewayPort.getVapidPublicKey()).thenReturn("test-public-key-123");
        when(gatewayPort.getProviderName()).thenReturn("MOCK");

        mockMvc.perform(get("/api/v1/notifications/web-push/vapid-public-key")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("test-public-key-123"))
                .andExpect(jsonPath("$.provider").value("MOCK"));
    }

    @Test
    void shouldReturnPreferencesForAuthenticatedUser() throws Exception {
        String userId = "user-jwt-1";
        WebPushPreferenceDto dto = new WebPushPreferenceDto(
                true,
                OffsetDateTime.now(ZoneOffset.UTC),
                new BigDecimal("25.00"),
                true
        );
        when(getPreferencesUseCase.execute(userId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/notifications/web-push/preferences")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedIn").value(true))
                .andExpect(jsonPath("$.minMarginOfSafetyPct").value(25.00))
                .andExpect(jsonPath("$.onlyCustodyTickers").value(true));
    }

    @Test
    void shouldUpdatePreferences() throws Exception {
        String userId = "user-jwt-1";
        WebPushPreferenceDto dto = new WebPushPreferenceDto(
                true,
                OffsetDateTime.now(ZoneOffset.UTC),
                new BigDecimal("30.00"),
                false
        );
        when(updatePreferencesUseCase.execute(eq(userId), any(UpdateWebPushPreferenceRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/notifications/web-push/preferences")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optedIn\":true,\"minMarginOfSafetyPct\":30.00,\"onlyCustodyTickers\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedIn").value(true))
                .andExpect(jsonPath("$.minMarginOfSafetyPct").value(30.00))
                .andExpect(jsonPath("$.onlyCustodyTickers").value(false));
    }

    @Test
    void shouldRegisterSubscription() throws Exception {
        String userId = "user-jwt-1";
        UUID subId = UUID.randomUUID();
        WebPushSubscriptionDto dto = new WebPushSubscriptionDto(
                subId,
                "https://fcm.googleapis.com/.../****",
                "Mozilla/5.0",
                true,
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        when(registerSubscriptionUseCase.execute(eq(userId), any(RegisterWebPushSubscriptionRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/notifications/web-push/subscriptions")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/token\",\"p256dhKey\":\"k1\",\"authKey\":\"a1\",\"userAgent\":\"Mozilla/5.0\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(subId.toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.endpointPreview").value("https://fcm.googleapis.com/.../****"));
    }

    @Test
    void shouldReturn400WhenRegisteringSubscriptionWithInvalidEndpoint() throws Exception {
        String userId = "user-jwt-1";
        when(registerSubscriptionUseCase.execute(eq(userId), any(RegisterWebPushSubscriptionRequest.class)))
                .thenThrow(new IllegalArgumentException("Endpoint must use HTTPS or localhost/127.0.0.1 for local development"));

        mockMvc.perform(post("/api/v1/notifications/web-push/subscriptions")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"http://insecure.com\",\"p256dhKey\":\"k1\",\"authKey\":\"a1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Endpoint must use HTTPS or localhost/127.0.0.1 for local development"));
    }

    @Test
    void shouldDeleteSubscription() throws Exception {
        String userId = "user-jwt-1";
        UUID subId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/notifications/web-push/subscriptions/" + subId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentSubscription() throws Exception {
        String userId = "user-jwt-1";
        UUID subId = UUID.randomUUID();
        doThrow(new WebPushSubscriptionNotFoundException("Assinatura Web Push não encontrada: " + subId))
                .when(deleteSubscriptionUseCase).execute(userId, subId);

        mockMvc.perform(delete("/api/v1/notifications/web-push/subscriptions/" + subId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Assinatura Web Push não encontrada: " + subId));
    }

    @Test
    void shouldSendTestAlertSuccessfully() throws Exception {
        String userId = "user-jwt-1";
        WebPushTestAlertResultDto result = new WebPushTestAlertResultDto(
                1,
                0,
                "MOCK",
                "Alerta de teste",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        when(sendTestAlertUseCase.execute(userId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/notifications/web-push/test")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.provider").value("MOCK"));
    }

    @Test
    void shouldReturnHistoryForAuthenticatedUser() throws Exception {
        String userId = "user-jwt-1";
        List<WebPushNotificationLogDto> history = List.of(
                new WebPushNotificationLogDto(
                        UUID.randomUUID(),
                        "PETR4",
                        "GRAHAM_BUY_ZONE",
                        "Zona de Compra",
                        "Mensagem de teste",
                        "MOCK",
                        201,
                        "SENT",
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC)
                )
        );
        when(getHistoryUseCase.execute(userId, 20)).thenReturn(history);

        mockMvc.perform(get("/api/v1/notifications/web-push/history")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/web-push/preferences"))
                .andExpect(status().isUnauthorized());
    }
}
