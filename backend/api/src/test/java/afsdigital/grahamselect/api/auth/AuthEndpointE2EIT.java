package afsdigital.grahamselect.api.auth;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes E2E via HTTP para a Story 1.1: Registro e Login via Google Sign-In.
 *
 * Usa um JwtDecoder mockado para que a requisição passe pelo BearerTokenAuthenticationFilter,
 * acionando nosso OAuth2UserProvisioningConverter real e testando o auto-provisioning de ponta a ponta.
 */
@AutoConfigureMockMvc
class AuthEndpointE2EIT extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final String PROTECTED_ENDPOINT = "/api/v1/ranked-companies";

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private void mockJwtDecoder(String tokenValue, String sub, String email, String name) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .claim("sub", sub)
                .claim("email", email)
                .claim("name", name)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);
    }

    // ==========================================
    // AC1: Login com Google autoriza acesso
    // ==========================================

    @Nested
    @DisplayName("AC1: Requisições autenticadas com JWT válido")
    class AuthenticatedRequestTests {

        @Test
        @DisplayName("Requisição com JWT válido deve retornar 200")
        void shouldReturn200WithValidJwt() throws Exception {
            String token = "valid.jwt.token";
            mockJwtDecoder(token, "google-sub-001", "user@example.com", "Test User");

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ==========================================
    // AC2: Auto-provisioning via HTTP
    // ==========================================

    @Nested
    @DisplayName("AC2: Auto-provisioning de usuário no primeiro acesso via HTTP")
    class AutoProvisioningViaHttpTests {

        @Test
        @DisplayName("Primeiro acesso via HTTP deve provisionar usuário no banco")
        void shouldProvisionUserOnFirstHttpRequest() throws Exception {
            assertThat(userRepository.count()).isZero();

            String token = "http.provisioning.token";
            mockJwtDecoder(token, "http-sub-001", "http-user@example.com", "HTTP User");

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            Optional<User> user = userRepository.findByGoogleSub("http-sub-001");
            assertThat(user).isPresent();
            assertThat(user.get().getEmail()).isEqualTo("http-user@example.com");
            assertThat(user.get().getFullName()).isEqualTo("HTTP User");
        }
    }

    // ==========================================
    // AC3: Idempotência do auto-provisioning
    // ==========================================

    @Nested
    @DisplayName("AC3: Idempotência — segundo acesso não duplica registro")
    class IdempotencyTests {

        @Test
        @DisplayName("Segundo acesso HTTP com mesmo sub não deve duplicar usuário")
        void shouldNotDuplicateUserOnSecondHttpRequest() throws Exception {
            String token = "idempotent.token";
            mockJwtDecoder(token, "idempotent-sub", "idempotent@example.com", "Idempotent User");

            // Primeiro acesso
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Segundo acesso
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            assertThat(userRepository.count()).isEqualTo(1);
        }
    }

    // ==========================================
    // AC4: JWT inválido/ausente → 401
    // ==========================================

    @Nested
    @DisplayName("AC4: Requisições sem/com JWT inválido")
    class UnauthorizedTests {

        @Test
        @DisplayName("Requisição sem JWT deve retornar 401")
        void shouldReturn401WithoutJwt() throws Exception {
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Requisição com JWT inválido (decoder lança erro) deve retornar 401")
        void shouldReturn401WithInvalidJwt() throws Exception {
            String token = "invalid.token";
            when(jwtDecoder.decode(anyString())).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("Invalid token"));

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==========================================
    // AC5: Endpoints públicos não exigem JWT
    // ==========================================

    @Nested
    @DisplayName("AC5: Endpoints públicos (/api/v1/public/**)")
    class PublicEndpointTests {

        @Test
        @DisplayName("Endpoint público deve estar acessível sem JWT (retorna 404, não 401)")
        void shouldNotRequireJwtForPublicEndpoints() throws Exception {
            mockMvc.perform(get("/api/v1/public/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    // ==========================================
    // AC6: SecurityContext contém ID interno do usuário
    // ==========================================

    @Nested
    @DisplayName("AC6: SecurityContext com ID interno (validação indireta)")
    class SecurityContextTests {

        @Test
        @DisplayName("Requisição autenticada provisiona usuário com ID interno correto")
        void shouldProvisionUserAndSetInternalIdAsPrincipal() throws Exception {
            String token = "context.token";
            mockJwtDecoder(token, "ctx-sub-001", "context@example.com", "Context User");

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            User user = userRepository.findByGoogleSub("ctx-sub-001").orElseThrow();
            assertThat(user.getId()).isNotNull();
            assertThat(user.getId()).isGreaterThan(0);
        }
    }
}
