package afsdigital.grahamselect.api.notifications.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.notifications.application.dto.UpdateWhatsappPreferenceRequest;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappNotificationLogDto;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappPreferenceDto;
import afsdigital.grahamselect.common.notifications.application.dto.WhatsappTestAlertResultDto;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWhatsappNotificationHistoryUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.GetWhatsappPreferencesUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.SendTestWhatsappAlertUseCase;
import afsdigital.grahamselect.common.notifications.application.usecase.UpdateWhatsappPreferencesUseCase;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WhatsappNotificationsControllerTest extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetWhatsappPreferencesUseCase getPreferencesUseCase;

    @MockBean
    private UpdateWhatsappPreferencesUseCase updatePreferencesUseCase;

    @MockBean
    private SendTestWhatsappAlertUseCase sendTestAlertUseCase;

    @MockBean
    private GetWhatsappNotificationHistoryUseCase getHistoryUseCase;

    @Test
    void shouldReturnPreferencesForAuthenticatedUser() throws Exception {
        String userId = "user-jwt-1";
        WhatsappPreferenceDto dto = new WhatsappPreferenceDto(
                "+5511999998888",
                true,
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                true,
                true,
                new BigDecimal("5.00")
        );
        when(getPreferencesUseCase.execute(userId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/notifications/whatsapp/preferences")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+5511999998888"))
                .andExpect(jsonPath("$.optedIn").value(true));
    }

    @Test
    void shouldReturn400ProblemDetailWhenPhoneIsInvalid() throws Exception {
        String userId = "user-jwt-1";
        when(updatePreferencesUseCase.execute(eq(userId), any(UpdateWhatsappPreferenceRequest.class)))
                .thenThrow(new IllegalArgumentException("Número de WhatsApp inválido. Utilize o formato E.164."));

        mockMvc.perform(put("/api/v1/notifications/whatsapp/preferences")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"1199999\",\"optedIn\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Número de WhatsApp inválido. Utilize o formato E.164."));
    }

    @Test
    void shouldSendTestAlertSuccessfully() throws Exception {
        String userId = "user-jwt-1";
        WhatsappTestAlertResultDto result = new WhatsappTestAlertResultDto(
                "SENT",
                "MOCK",
                "mock-wamid-123",
                "Teste OK",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        when(sendTestAlertUseCase.execute(userId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/notifications/whatsapp/test")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.provider").value("MOCK"));
    }

    @Test
    void shouldReturnHistoryForAuthenticatedUser() throws Exception {
        String userId = "user-jwt-1";
        List<WhatsappNotificationLogDto> history = List.of(
                new WhatsappNotificationLogDto(
                        UUID.randomUUID(),
                        "TEST",
                        "Mensagem de teste",
                        "MOCK",
                        "SENT",
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC)
                )
        );
        when(getHistoryUseCase.execute(userId, 20)).thenReturn(history);

        mockMvc.perform(get("/api/v1/notifications/whatsapp/history")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("TEST"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/whatsapp/preferences"))
                .andExpect(status().isUnauthorized());
    }
}
