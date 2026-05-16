package afsdigital.grahamselect.api.user;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
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

        // 1. Get profile (should be null)
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investorProfile").isEmpty());

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

    private User createAndMockUser(String sub, String email) {
        User user = User.builder()
                .googleSub(sub)
                .email(email)
                .fullName("Test User")
                .tier(SubscriptionTier.FREE)
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
