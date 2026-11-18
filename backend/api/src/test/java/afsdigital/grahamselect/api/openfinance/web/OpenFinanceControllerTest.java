package afsdigital.grahamselect.api.openfinance.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessGuard;
import afsdigital.grahamselect.common.openfinance.application.dto.ConsentUrlResponseDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.ExchangeConsentCallbackDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.InitiateConnectionRequestDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceConnectionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceInstitutionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceSyncResultDTO;
import afsdigital.grahamselect.common.openfinance.application.usecase.CompleteOpenFinanceConsentUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.InitiateOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.ListOpenFinanceInstitutionsUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.ListUserOpenFinanceConnectionsUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.RevokeOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.application.usecase.SyncOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OpenFinanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ListOpenFinanceInstitutionsUseCase listInstitutionsUseCase;

    @Mock
    private InitiateOpenFinanceConnectionUseCase initiateConnectionUseCase;

    @Mock
    private CompleteOpenFinanceConsentUseCase completeConsentUseCase;

    @Mock
    private SyncOpenFinanceConnectionUseCase syncConnectionUseCase;

    @Mock
    private RevokeOpenFinanceConnectionUseCase revokeConnectionUseCase;

    @Mock
    private ListUserOpenFinanceConnectionsUseCase listUserConnectionsUseCase;

    @Mock
    private PremiumFeatureAccessGuard premiumFeatureAccessGuard;

    @InjectMocks
    private OpenFinanceController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Jwt mockJwt = Jwt.withTokenValue("mock-jwt-token")
            .header("alg", "none")
            .claim("sub", "user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "configuredWebhookSecret", "test-webhook-secret-xyz");

        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(Jwt.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockJwt;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/open-finance/institutions should return 200 and institution list")
    void testListInstitutions() throws Exception {
        when(listInstitutionsUseCase.execute()).thenReturn(List.of(
                new OpenFinanceInstitutionDTO("XP_INVESTIMENTOS", "XP", "http://logo.png", true, "Desc")
        ));

        mockMvc.perform(get("/api/v1/open-finance/institutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("XP_INVESTIMENTOS"));
    }

    @Test
    @DisplayName("GET /api/v1/open-finance/connections should return 200 and user connections")
    void testListConnections() throws Exception {
        UUID id = UUID.randomUUID();
        when(listUserConnectionsUseCase.execute("user-123")).thenReturn(List.of(
                new OpenFinanceConnectionDTO(
                        id, "user-123", null, "BTG_PACTUAL", "BTG Pactual", "http://logo",
                        OpenFinanceConnectionStatus.ACTIVE, null, null, null, null, null, null
                )
        ));

        mockMvc.perform(get("/api/v1/open-finance/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].institutionId").value("BTG_PACTUAL"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/connections should return 201 and consent URL")
    void testInitiateConnection() throws Exception {
        UUID connectionId = UUID.randomUUID();
        InitiateConnectionRequestDTO request = new InitiateConnectionRequestDTO("XP_INVESTIMENTOS", null);

        when(initiateConnectionUseCase.execute(eq("user-123"), any(), any()))
                .thenReturn(new ConsentUrlResponseDTO(connectionId, "https://auth.xp.com.br", "state-xyz"));

        mockMvc.perform(post("/api/v1/open-finance/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.connectionId").value(connectionId.toString()))
                .andExpect(jsonPath("$.consentUrl").value("https://auth.xp.com.br"));
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/connections with profileId should check premium guard")
    void testInitiateConnectionWithProfileId() throws Exception {
        UUID connectionId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        InitiateConnectionRequestDTO request = new InitiateConnectionRequestDTO("XP_INVESTIMENTOS", profileId);

        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(true);
        when(initiateConnectionUseCase.execute(eq("user-123"), any(), any()))
                .thenReturn(new ConsentUrlResponseDTO(connectionId, "https://auth.xp.com.br", "state-xyz"));

        mockMvc.perform(post("/api/v1/open-finance/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(premiumFeatureAccessGuard).canAccessPremiumFeatures();
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/connections with profileId when not premium should return 403")
    void testInitiateConnectionWithProfileIdNotPremium() throws Exception {
        UUID profileId = UUID.randomUUID();
        InitiateConnectionRequestDTO request = new InitiateConnectionRequestDTO("XP_INVESTIMENTOS", profileId);

        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(false);

        mockMvc.perform(post("/api/v1/open-finance/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/connections/callback should activate connection and return 200")
    void testCompleteConsentCallback() throws Exception {
        UUID connectionId = UUID.randomUUID();
        ExchangeConsentCallbackDTO callback = new ExchangeConsentCallbackDTO(connectionId, "code-123", "state-123");

        when(completeConsentUseCase.execute(eq("user-123"), any(), any()))
                .thenReturn(new OpenFinanceConnectionDTO(
                        connectionId, "user-123", null, "XP_INVESTIMENTOS", "XP", "http://logo",
                        OpenFinanceConnectionStatus.ACTIVE, null, null, null, null, null, null
                ));

        mockMvc.perform(post("/api/v1/open-finance/connections/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/connections/{id}/sync should trigger manual sync")
    void testSyncConnection() throws Exception {
        UUID connectionId = UUID.randomUUID();

        when(syncConnectionUseCase.execute(eq(connectionId), eq("user-123"), eq(OpenFinanceSyncType.MANUAL)))
                .thenReturn(new OpenFinanceSyncResultDTO(
                        connectionId, OpenFinanceSyncStatus.SUCCESS, 2, 5, 4, 1,
                        "Sincronização realizada com sucesso.", LocalDateTime.now(ZoneOffset.UTC)
                ));

        mockMvc.perform(post("/api/v1/open-finance/connections/" + connectionId + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradesCreated").value(4))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("DELETE /api/v1/open-finance/connections/{id} should revoke connection and return 204")
    void testRevokeConnection() throws Exception {
        UUID connectionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/open-finance/connections/" + connectionId))
                .andExpect(status().isNoContent());

        verify(revokeConnectionUseCase).execute(connectionId, "user-123");
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/webhook with valid secret should return 200")
    void testWebhookValidSecret() throws Exception {
        mockMvc.perform(post("/api/v1/open-finance/webhook")
                        .header("X-Webhook-Secret", "test-webhook-secret-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("event", "CONSENT_UPDATED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/open-finance/webhook with invalid or missing secret should return 401")
    void testWebhookInvalidSecret() throws Exception {
        mockMvc.perform(post("/api/v1/open-finance/webhook")
                        .header("X-Webhook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("event", "CONSENT_UPDATED"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/open-finance/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("event", "CONSENT_UPDATED"))))
                .andExpect(status().isUnauthorized());
    }
}
