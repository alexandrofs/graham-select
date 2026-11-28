package afsdigital.grahamselect.api.cerrado;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.ai.application.service.AiDiagnosticCacheService;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class CerradoPreferencesIT extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiDiagnosticCacheService aiDiagnosticCacheService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_ai_diagnostic_cache");
        jdbcTemplate.update("DELETE FROM tb_cerrado_preferences");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/cerrado/templates should return 401 when unauthenticated and 200 with 3 official templates when authenticated")
    void shouldReturnTemplatesWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/cerrado/templates"))
                .andExpect(status().isUnauthorized());

        User user = createUser("cerrado-tpl-sub", "tpl@kerno.test");
        mockJwt("tpl-token", user);

        mockMvc.perform(get("/api/v1/cerrado/templates")
                        .header("Authorization", "Bearer tpl-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultNeutralGrade").value(6))
                .andExpect(jsonPath("$.cvmDisclaimer").isNotEmpty())
                .andExpect(jsonPath("$.templates", hasSize(3)))
                .andExpect(jsonPath("$.templates[0].code").value("EQUILIBRADO_CERRADO"))
                .andExpect(jsonPath("$.templates[0].targetAcoesBr").value(39.00))
                .andExpect(jsonPath("$.templates[0].targetFiis").value(30.00))
                .andExpect(jsonPath("$.templates[0].targetRendaFixa").value(20.00))
                .andExpect(jsonPath("$.templates[0].targetInternacional").value(10.00))
                .andExpect(jsonPath("$.templates[0].targetCripto").value(1.00))
                .andExpect(jsonPath("$.templates[1].code").value("MODERADO_PATRIMONIAL"))
                .andExpect(jsonPath("$.templates[1].targetRendaFixa").value(45.00))
                .andExpect(jsonPath("$.templates[1].targetCripto").value(0.00))
                .andExpect(jsonPath("$.templates[2].code").value("ACUMULACAO_ARROJADA"))
                .andExpect(jsonPath("$.templates[2].targetAcoesBr").value(45.00))
                .andExpect(jsonPath("$.templates[2].targetCripto").value(5.00));
    }

    @Test
    @DisplayName("GET /api/v1/cerrado/preferences should return 404 ProblemDetail when absent and 401 when unauthenticated")
    void shouldReturn404WhenPreferencesAbsent() throws Exception {
        mockMvc.perform(get("/api/v1/cerrado/preferences"))
                .andExpect(status().isUnauthorized());

        User user = createUser("cerrado-404-sub", "404@kerno.test");
        mockJwt("token-404", user);

        mockMvc.perform(get("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer token-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Cerrado Preferences Not Found"));
    }

    @Test
    @DisplayName("PUT /api/v1/cerrado/preferences should insert, update preserving Graham defaults/overrides, and invalidate AI cache")
    void shouldInsertUpdateAndInvalidateAiCache() throws Exception {
        User user = createUser("cerrado-upsert-sub", "upsert@kerno.test");
        mockJwt("upsert-token", user);

        aiDiagnosticCacheService.saveDiagnostic(
                user.getId(),
                "hash-before-insert",
                "gemini-2.5-flash",
                "v1",
                "{\"summary\":\"cached\"}"
        );

        // 1. Insert without Graham fields -> defaults 1.30 / true
        String insertPayload = """
                {
                  "targetAcoesBr": 39.00,
                  "targetFiis": 30.00,
                  "targetRendaFixa": 20.00,
                  "targetInternacional": 10.00,
                  "targetCripto": 1.00
                }
                """;

        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer upsert-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(insertPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.targetAcoesBr").value(39.00))
                .andExpect(jsonPath("$.grahamMultiplier").value(1.30))
                .andExpect(jsonPath("$.grahamFilterEnabled").value(true))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(countAiCacheRows(user.getId())).isZero();

        // 2. Update with explicit Graham values
        String updateWithGrahamPayload = """
                {
                  "targetAcoesBr": 45.00,
                  "targetFiis": 25.00,
                  "targetRendaFixa": 10.00,
                  "targetInternacional": 15.00,
                  "targetCripto": 5.00,
                  "grahamMultiplier": 1.50,
                  "grahamFilterEnabled": false
                }
                """;

        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer upsert-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateWithGrahamPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grahamMultiplier").value(1.50))
                .andExpect(jsonPath("$.grahamFilterEnabled").value(false));

        // Re-seed AI cache and update omitting Graham fields -> must preserve 1.50 / false and invalidate cache
        aiDiagnosticCacheService.saveDiagnostic(
                user.getId(),
                "hash-before-second-update",
                "gemini-2.5-flash",
                "v1",
                "{\"summary\":\"cached-2\"}"
        );

        String updateOmittingGrahamPayload = """
                {
                  "targetAcoesBr": 20.00,
                  "targetFiis": 25.00,
                  "targetRendaFixa": 45.00,
                  "targetInternacional": 10.00,
                  "targetCripto": 0.00
                }
                """;

        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer upsert-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateOmittingGrahamPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRendaFixa").value(45.00))
                .andExpect(jsonPath("$.grahamMultiplier").value(1.50))
                .andExpect(jsonPath("$.grahamFilterEnabled").value(false));

        assertThat(countAiCacheRows(user.getId())).isZero();
        assertThat(countPreferencesRows(user.getId())).isEqualTo(1);

        // 3. GET reflects latest persisted state
        mockMvc.perform(get("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer upsert-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRendaFixa").value(45.00))
                .andExpect(jsonPath("$.grahamMultiplier").value(1.50))
                .andExpect(jsonPath("$.grahamFilterEnabled").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/cerrado/preferences should return 422 ProblemDetail on invalid sum, null, scale > 2, or invalid grahamMultiplier without touching DB or AI cache")
    void shouldReturn422AndPreserveAiCacheOnInvalidRequests() throws Exception {
        User user = createUser("cerrado-422-sub", "422@kerno.test");
        mockJwt("token-422", user);

        aiDiagnosticCacheService.saveDiagnostic(
                user.getId(),
                "hash-intact",
                "gemini-2.5-flash",
                "v1",
                "{\"summary\":\"must-stay\"}"
        );

        String[] invalidPayloads = {
                // Sum = 98.00%
                """
                {"targetAcoesBr":38.00,"targetFiis":30.00,"targetRendaFixa":20.00,"targetInternacional":10.00,"targetCripto":0.00}
                """,
                // Null field
                """
                {"targetAcoesBr":null,"targetFiis":30.00,"targetRendaFixa":40.00,"targetInternacional":20.00,"targetCripto":10.00}
                """,
                // Scale > 2
                """
                {"targetAcoesBr":33.333,"targetFiis":26.667,"targetRendaFixa":20.00,"targetInternacional":10.00,"targetCripto":10.00}
                """,
                // grahamMultiplier > 99.99
                """
                {"targetAcoesBr":39.00,"targetFiis":30.00,"targetRendaFixa":20.00,"targetInternacional":10.00,"targetCripto":1.00,"grahamMultiplier":100.00}
                """,
                // grahamMultiplier scale > 2
                """
                {"targetAcoesBr":39.00,"targetFiis":30.00,"targetRendaFixa":20.00,"targetInternacional":10.00,"targetCripto":1.00,"grahamMultiplier":1.333}
                """
        };

        for (String payload : invalidPayloads) {
            mockMvc.perform(put("/api/v1/cerrado/preferences")
                            .header("Authorization", "Bearer token-422")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.title").value("Cerrado Validation Error"));
        }

        assertThat(countPreferencesRows(user.getId())).isZero();
        assertThat(countAiCacheRows(user.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Multi-tenant isolation: User B actions must not alter User A preferences or AI cache")
    void shouldIsolatePreferencesAndAiCacheBetweenUsers() throws Exception {
        User userA = createUser("cerrado-user-a", "usera@kerno.test");
        User userB = createUser("cerrado-user-b", "userb@kerno.test");
        mockJwt("token-a", userA);
        mockJwt("token-b", userB);

        String payloadA = """
                {
                  "targetAcoesBr": 39.00,
                  "targetFiis": 30.00,
                  "targetRendaFixa": 20.00,
                  "targetInternacional": 10.00,
                  "targetCripto": 1.00,
                  "grahamMultiplier": 1.40
                }
                """;
        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer token-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadA))
                .andExpect(status().isOk());

        aiDiagnosticCacheService.saveDiagnostic(
                userA.getId(),
                "hash-user-a",
                "gemini-2.5-flash",
                "v1",
                "{\"summary\":\"user-a-cache\"}"
        );

        String payloadB = """
                {
                  "targetAcoesBr": 20.00,
                  "targetFiis": 25.00,
                  "targetRendaFixa": 45.00,
                  "targetInternacional": 10.00,
                  "targetCripto": 0.00,
                  "grahamMultiplier": 1.20
                }
                """;
        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer token-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userB.getId()))
                .andExpect(jsonPath("$.targetRendaFixa").value(45.00));

        // User A's AI cache and preferences remain untouched
        assertThat(countAiCacheRows(userA.getId())).isEqualTo(1);

        mockMvc.perform(get("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer token-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userA.getId()))
                .andExpect(jsonPath("$.targetAcoesBr").value(39.00))
                .andExpect(jsonPath("$.grahamMultiplier").value(1.40));
    }

    @Test
    @DisplayName("Should return 401 when Authentication.getName() is non-numeric")
    void shouldReturn401WhenAuthenticationNameIsNonNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/cerrado/preferences")
                        .with(user("not-a-number")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        User user = createUser("cerrado-400-sub", "400@kerno.test");
        mockJwt("token-400", user);

        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer token-400")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/cerrado/preferences")
                        .header("Authorization", "Bearer token-400")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String sub, String email) {
        return userRepository.save(User.builder()
                .googleSub(sub)
                .email(email)
                .fullName("Cerrado IT User")
                .tier(SubscriptionTier.TRIAL)
                .commercialPlan(CommercialPlan.PRO_TRIAL)
                .build());
    }

    private void mockJwt(String token, User user) {
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", user.getGoogleSub())
                .claim("email", user.getEmail())
                .claim("name", user.getId().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(token)).thenReturn(jwt);
    }

    private int countAiCacheRows(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_ai_diagnostic_cache WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count != null ? count : 0;
    }

    private int countPreferencesRows(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_cerrado_preferences WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count != null ? count : 0;
    }
}
