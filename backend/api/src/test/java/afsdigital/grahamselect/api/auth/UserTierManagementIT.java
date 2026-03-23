package afsdigital.grahamselect.api.auth;

import afsdigital.grahamselect.api.auth.infrastructure.security.OAuth2UserProvisioningConverter;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class UserTierManagementIT extends BaseRepositoryIT {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OAuth2UserProvisioningConverter converter;

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private JwtDecoder jwtDecoder;

        @BeforeEach
        void cleanUp() {
                userRepository.deleteAll();
        }

        @Test
        @DisplayName("Should provision user with TRIAL tier and 30 days expiration")
        void shouldProvisionUserWithTrial() {
                String googleId = "trial-user-123";
                String email = "trial@example.com";

                Jwt jwt = Jwt.withTokenValue("mock-token")
                                .header("alg", "none")
                                .subject(googleId)
                                .claim("email", email)
                                .claim("name", "Trial User")
                                .build();

                // Act
                converter.convert(jwt);

                // Assert
                Optional<User> userOpt = userRepository.findByGoogleSub(googleId);
                assertThat(userOpt).isPresent();
                User user = userOpt.get();
                assertThat(user.getTier()).isEqualTo(SubscriptionTier.TRIAL);
                assertThat(user.getTrialEndsAt()).isAfter(LocalDateTime.now().plusDays(29));
        }

        @Test
        @DisplayName("Should return user profile via /users/me")
        void shouldReturnUserProfile() throws Exception {
                // Given - a user exists
                User user = User.builder()
                                .googleSub("existing-sub")
                                .email("existing@example.com")
                                .fullName("Existing User")
                                .tier(SubscriptionTier.TRIAL)
                                .trialEndsAt(LocalDateTime.now().plusDays(10).plusHours(1))
                                .build();
                user = userRepository.save(user);

                // Mock JWT decoder to return a JWT that the converter will map to this user
                String token = "api.test.token";
                Jwt jwt = Jwt.withTokenValue(token)
                                .header("alg", "RS256")
                                .claim("sub", user.getGoogleSub())
                                .claim("email", user.getEmail())
                                .claim("name", user.getFullName())
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
                when(jwtDecoder.decode(token)).thenReturn(jwt);

                // Act & Assert
                mockMvc.perform(get("/api/v1/users/me")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("existing@example.com"))
                                .andExpect(jsonPath("$.tier").value("TRIAL"))
                                .andExpect(jsonPath("$.daysRemaining").value(10));
        }

        @Test
        @DisplayName("Should return FREE tier if TRIAL is expired")
        void shouldReturnFreeTierIfTrialIsExpired() throws Exception {
                // Given - a user with expired trial
                User user = User.builder()
                                .googleSub("expired-sub")
                                .email("expired@example.com")
                                .fullName("Expired User")
                                .tier(SubscriptionTier.TRIAL)
                                .trialEndsAt(LocalDateTime.now().minusDays(1))
                                .build();
                user = userRepository.save(user);

                String token = "api.test.token.expired";
                Jwt jwt = Jwt.withTokenValue(token)
                                .header("alg", "RS256")
                                .claim("sub", user.getGoogleSub())
                                .claim("email", user.getEmail())
                                .claim("name", user.getFullName())
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
                when(jwtDecoder.decode(token)).thenReturn(jwt);

                // Act & Assert
                mockMvc.perform(get("/api/v1/users/me")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tier").value("FREE"))
                                .andExpect(jsonPath("$.daysRemaining").value(0));
        }

        @Test
        @DisplayName("Should return PREMIUM tier and ignore expiration")
        void shouldReturnPremiumTier() throws Exception {
                // Given - a user with PREMIUM tier and a past expiration date (not used for
                // PREMIUM)
                User user = User.builder()
                                .googleSub("premium-sub")
                                .email("premium@example.com")
                                .fullName("Premium User")
                                .tier(SubscriptionTier.PREMIUM)
                                .trialEndsAt(LocalDateTime.now().minusDays(10))
                                .build();
                user = userRepository.save(user);

                String token = "api.test.token.premium";
                Jwt jwt = Jwt.withTokenValue(token)
                                .header("alg", "RS256")
                                .claim("sub", user.getGoogleSub())
                                .claim("email", user.getEmail())
                                .claim("name", user.getFullName())
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
                when(jwtDecoder.decode(token)).thenReturn(jwt);

                // Act & Assert
                mockMvc.perform(get("/api/v1/users/me")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tier").value("PREMIUM"))
                                .andExpect(jsonPath("$.daysRemaining").value(0));
        }
}
