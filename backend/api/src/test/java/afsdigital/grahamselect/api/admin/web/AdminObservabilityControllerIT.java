package afsdigital.grahamselect.api.admin.web;

import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.entities.B3ImportStatusEntity;
import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.repository.B3ImportStatusJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories.IngestionAuditRepository;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatusState;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminObservabilityControllerIT extends BaseRepositoryIT {

    private static final String OBSERVABILITY_ENDPOINT = "/api/v1/admin/observability";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private B3ImportStatusJpaRepository b3ImportStatusJpaRepository;

    @Autowired
    private IngestionAuditRepository ingestionAuditRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        b3ImportStatusJpaRepository.deleteAll();
        ingestionAuditRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 200 with metrics and anonymized data when user has ADMIN role in JWT")
    void shouldReturn200WithMetricsAndAnonymizedDataWhenUserHasAdminRoleInJwt() throws Exception {
        User investor = userRepository.save(User.builder()
                .googleSub("investor-sub-1")
                .email("investidor.secreto@example.com")
                .fullName("Investidor Secreto")
                .tier(SubscriptionTier.PREMIUM)
                .role(UserRole.USER)
                .build());

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        b3ImportStatusJpaRepository.save(B3ImportStatusEntity.builder()
                .correlationId("corr-it-001")
                .userId(investor.getId().toString())
                .fileName("Extrato_Investidor_Secreto_CPF_999.xlsx")
                .status(B3ImportStatusState.COMPLETED)
                .processedRows(25)
                .successfulRows(25)
                .failedRows(0)
                .duplicatedRows(0)
                .createdAt(nowUtc.minusMinutes(15))
                .updatedAt(nowUtc.minusMinutes(14))
                .build());

        Jwt adminJwt = Jwt.withTokenValue("admin-jwt-token")
                .header("alg", "RS256")
                .claim("sub", "admin-jwt-sub")
                .claim("email", "admin@grahamselect.com")
                .claim("name", "System Admin")
                .claim("role", "ADMIN")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("admin-jwt-token")).thenReturn(adminJwt);

        mockMvc.perform(get(OBSERVABILITY_ENDPOINT)
                        .header("Authorization", "Bearer admin-jwt-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userMetrics.totalUsers").value(2))
                .andExpect(jsonPath("$.data.userMetrics.activeUsers24h").isNumber())
                .andExpect(jsonPath("$.data.uploadMetrics.totalUploads").value(1))
                .andExpect(jsonPath("$.data.uploadMetrics.successCount").value(1))
                .andExpect(jsonPath("$.data.grahamLatencyMetrics.slaTargetMs").value(2000))
                .andExpect(jsonPath("$.data.grahamLatencyMetrics.withinSla").value(true))
                .andExpect(jsonPath("$.data.recentUploads[0].anonymizedUserId").value(containsString("usr_***")))
                .andExpect(jsonPath("$.data.recentUploads[0].sanitizedFileName").value("extrato_b3_***.xlsx"))
                .andExpect(jsonPath("$.data.recentUploads[0].maskedPortfolioValue").value("***"))
                .andExpect(content().string(not(containsString("investidor.secreto@example.com"))))
                .andExpect(content().string(not(containsString("Investidor Secreto"))))
                .andExpect(content().string(not(containsString("CPF_999"))));
    }

    @Test
    @DisplayName("Should return 200 when user has ADMIN role persisted in database")
    void shouldReturn200WhenUserHasAdminRoleInDatabase() throws Exception {
        User dbAdmin = userRepository.save(User.builder()
                .googleSub("db-admin-sub")
                .email("dbadmin@grahamselect.com")
                .fullName("DB Admin")
                .tier(SubscriptionTier.FREE)
                .role(UserRole.ADMIN)
                .build());

        Jwt jwt = Jwt.withTokenValue("db-admin-token")
                .header("alg", "RS256")
                .claim("sub", dbAdmin.getGoogleSub())
                .claim("email", dbAdmin.getEmail())
                .claim("name", dbAdmin.getFullName())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("db-admin-token")).thenReturn(jwt);

        mockMvc.perform(get(OBSERVABILITY_ENDPOINT)
                        .header("Authorization", "Bearer db-admin-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userMetrics.totalUsers").value(1));
    }

    @Test
    @DisplayName("Should return 403 Forbidden ProblemDetail when user is a regular USER")
    void shouldReturn403ForbiddenProblemDetailWhenUserIsRegularUser() throws Exception {
        User regularUser = userRepository.save(User.builder()
                .googleSub("regular-user-sub")
                .email("regular@grahamselect.com")
                .fullName("Regular User")
                .tier(SubscriptionTier.PREMIUM)
                .role(UserRole.USER)
                .build());

        Jwt jwt = Jwt.withTokenValue("regular-token")
                .header("alg", "RS256")
                .claim("sub", regularUser.getGoogleSub())
                .claim("email", regularUser.getEmail())
                .claim("name", regularUser.getFullName())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("regular-token")).thenReturn(jwt);

        mockMvc.perform(get(OBSERVABILITY_ENDPOINT)
                        .header("Authorization", "Bearer regular-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:access-denied"))
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when request is unauthenticated")
    void shouldReturn401UnauthorizedWhenUnauthenticated() throws Exception {
        mockMvc.perform(get(OBSERVABILITY_ENDPOINT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
