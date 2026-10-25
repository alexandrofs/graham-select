package afsdigital.grahamselect.api.dlq.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.entities.DlqMessageEntity;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.repositories.DlqMessageRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminDlqControllerIT extends BaseRepositoryIT {

    private static final String DLQ_BASE_URL = "/api/v1/admin/dlq";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DlqMessageRepository dlqMessageRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        dlqMessageRepository.deleteAll();
        userRepository.deleteAll();
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    private void mockAdminJwt(String token) {
        Jwt adminJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", "admin-sub")
                .claim("email", "admin@grahamselect.com")
                .claim("name", "Admin User")
                .claim("role", "ADMIN")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(token)).thenReturn(adminJwt);
    }

    private void mockRegularUserJwt(String token) {
        Jwt userJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", "regular-user-sub")
                .claim("email", "user@grahamselect.com")
                .claim("name", "Regular User")
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(token)).thenReturn(userJwt);
    }

    @Test
    @DisplayName("Deve retornar 200 com lista de falhas quando autenticado como ADMIN")
    void shouldReturn200WithDlqFailuresWhenAdmin() throws Exception {
        mockAdminJwt("admin-token");

        DlqMessageEntity dlq1 = DlqMessageEntity.builder()
                .eventId("ev-1")
                .correlationId("corr-1")
                .userId("raw-user-1")
                .fileName("extrato1.xlsx")
                .lineNumber(10L)
                .errorMessage("Erro no formato numérico")
                .originalPayload("{\"ticker\":\"PETR4\",\"quantidade\":\"100\",\"preco\":\"30.00\",\"data\":\"10/01/2024\"}")
                .originalTopic(TopicConstants.TRADE_EXTRACTED_TOPIC)
                .status(DlqMessageStatus.PENDING)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        dlqMessageRepository.save(dlq1);

        mockMvc.perform(get(DLQ_BASE_URL)
                        .header("Authorization", "Bearer admin-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fileName").value("extrato1.xlsx"))
                .andExpect(jsonPath("$.data[0].errorMessage").value("Erro no formato numérico"))
                .andExpect(jsonPath("$.data[0].anonymizedUserId").value(containsString("usr_***")));
    }

    @Test
    @DisplayName("Deve reprocessar com sucesso mensagem individual quando ADMIN")
    void shouldReprocessDlqMessageSuccessfullyWhenAdmin() throws Exception {
        mockAdminJwt("admin-token");

        DlqMessageEntity dlq = DlqMessageEntity.builder()
                .eventId("ev-reprocess-1")
                .correlationId("corr-reprocess-1")
                .userId("raw-user-reprocess")
                .fileName("extrato_reprocess.xlsx")
                .lineNumber(12L)
                .errorMessage("Preço com símbolo R$")
                .originalPayload("{\"ticker\":\"BBAS3\",\"quantidade\":\"50\",\"preco\":\"R$ 28,40\",\"data\":\"20/02/2024\"}")
                .originalTopic(TopicConstants.TRADE_EXTRACTED_TOPIC)
                .status(DlqMessageStatus.PENDING)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        DlqMessageEntity saved = dlqMessageRepository.save(dlq);

        mockMvc.perform(post(DLQ_BASE_URL + "/" + saved.getId() + "/reprocess")
                        .header("Authorization", "Bearer admin-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.message").value(containsString("sucesso")));
    }

    @Test
    @DisplayName("Deve reprocessar em lote mensagens selecionadas quando ADMIN")
    void shouldReprocessBatchDlqMessagesSuccessfullyWhenAdmin() throws Exception {
        mockAdminJwt("admin-token");

        DlqMessageEntity dlq1 = DlqMessageEntity.builder()
                .eventId("ev-batch-1")
                .correlationId("corr-batch-1")
                .userId("raw-user-b1")
                .fileName("extrato_batch1.xlsx")
                .errorMessage("Falha de teste em lote")
                .originalPayload("{\"ticker\":\"ITSA4\",\"quantidade\":\"100\",\"preco\":\"10.50\",\"data\":\"15/03/2024\"}")
                .status(DlqMessageStatus.PENDING)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        DlqMessageEntity saved1 = dlqMessageRepository.save(dlq1);

        mockMvc.perform(post(DLQ_BASE_URL + "/reprocess-batch")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [" + saved1.getId() + "]}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProcessed").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden com RFC 7807 quando usuário comum tentar acessar")
    void shouldReturn403ForbiddenWhenRegularUser() throws Exception {
        mockRegularUserJwt("user-token");

        mockMvc.perform(get(DLQ_BASE_URL)
                        .header("Authorization", "Bearer user-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando não autenticado")
    void shouldReturn401UnauthorizedWhenUnauthenticated() throws Exception {
        mockMvc.perform(get(DLQ_BASE_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found com RFC 7807 quando id da DLQ não existir")
    void shouldReturn404NotFoundWhenDlqIdDoesNotExist() throws Exception {
        mockAdminJwt("admin-token");

        mockMvc.perform(post(DLQ_BASE_URL + "/999999/reprocess")
                        .header("Authorization", "Bearer admin-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("DLQ Message Not Found"))
                .andExpect(jsonPath("$.detail").value(containsString("999999")));
    }
}
