package afsdigital.grahamselect.api.user;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class UserProfileIT extends BaseRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should update investor profile via PATCH")
    void shouldUpdateInvestorProfile() throws Exception {
        User user = createAndMockUser("user-1", "user1@example.com");

        String token = "mock-token";
        mockJwt(token, user);

        // 1. Get profile (should be null investorProfile and expose termsAcceptedAt + commercialPlan)
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investorProfile").isEmpty())
                .andExpect(jsonPath("$.commercialPlan").value("FREE"))
                .andExpect(jsonPath("$.termsAcceptedAt").isNotEmpty());

        // 2. Update profile
        String requestBody = """
                {
                    "investorProfile": "AGGRESSIVE"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investorProfile").value("AGGRESSIVE"));

        // 3. Verify in database
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getInvestorProfile().name()).isEqualTo("AGGRESSIVE");
    }

    @Test
    @DisplayName("Should expose PRO_TRIAL and FOUNDER_LIFETIME commercial plan fields via /api/v1/users/me")
    void shouldExposeCommercialPlanFieldsInUsersMe() throws Exception {
        java.time.LocalDateTime nowUtc = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        User trialUser = userRepository.save(User.builder()
                .googleSub("trial-sub")
                .email("trial@example.com")
                .fullName("Trial User")
                .tier(SubscriptionTier.TRIAL)
                .commercialPlan(CommercialPlan.PRO_TRIAL)
                .trialEndsAt(nowUtc.plusDays(90))
                .trialExpiresAt(nowUtc.plusDays(90))
                .termsAcceptedAt(nowUtc)
                .build());
        mockJwt("trial-token", trialUser);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer trial-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPlan").value("PRO_TRIAL"))
                .andExpect(jsonPath("$.trialExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.daysRemaining").value(org.hamcrest.Matchers.greaterThanOrEqualTo(89)))
                .andExpect(jsonPath("$.termsAcceptedAt").isNotEmpty());

        User founderUser = userRepository.save(User.builder()
                .googleSub("founder-sub")
                .email("founder@example.com")
                .fullName("Founder User")
                .tier(SubscriptionTier.PREMIUM)
                .commercialPlan(CommercialPlan.FOUNDER_LIFETIME)
                .termsAcceptedAt(nowUtc)
                .build());
        mockJwt("founder-token", founderUser);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer founder-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPlan").value("FOUNDER_LIFETIME"))
                .andExpect(jsonPath("$.daysRemaining").value(0))
                .andExpect(jsonPath("$.termsAcceptedAt").isNotEmpty());
    }

    private User createAndMockUser(String sub, String email) {
        User user = User.builder()
                .googleSub(sub)
                .email(email)
                .fullName("Test User")
                .tier(SubscriptionTier.FREE)
                .commercialPlan(CommercialPlan.FREE)
                .termsAcceptedAt(java.time.LocalDateTime.of(2026, 11, 24, 12, 0))
                .build();
        return userRepository.save(user);
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
}
