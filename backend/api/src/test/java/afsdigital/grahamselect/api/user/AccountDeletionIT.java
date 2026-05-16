package afsdigital.grahamselect.api.user;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.DeletionStatus;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.common.user.application.repository.AccountDeletionRequestRepository;
import afsdigital.grahamselect.api.user.infrastructure.spring.AccountPurgeScheduler;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AccountDeletionIT extends BaseRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountDeletionRequestRepository deletionRequestRepository;

    @Autowired
    private AccountPurgeScheduler purgeScheduler;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Full flow: request deletion, cancel it")
    void shouldRequestAndCancelDeletion() throws Exception {
        User user = createAndMockUser("user-1", "user1@example.com");

        String token = "mock-token";
        mockJwt(token, user);

        // 1. Request deletion
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Optional<AccountDeletionRequest> requestOpt = deletionRequestRepository.findPendingByUserId(user.getId());
        assertThat(requestOpt).isPresent();

        // 2. Cancel deletion
        mockMvc.perform(post("/api/v1/users/me/cancel-deletion")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        requestOpt = deletionRequestRepository.findById(requestOpt.get().getId());
        assertThat(requestOpt).isPresent();
        assertThat(requestOpt.get().getStatus()).isEqualTo(DeletionStatus.CANCELLED);
    }

    @Test
    @DisplayName("Full flow: request deletion, run purge job")
    void shouldPerformHardDeleteDuringPurge() throws Exception {
        User user = createAndMockUser("purge-user", "purge@example.com");
        String token = "purge-token";
        mockJwt(token, user);

        // 1. Request deletion
        String responseContent = mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        
        long requestId = Long.parseLong(com.jayway.jsonpath.JsonPath.read(responseContent, "$.data.requestId").toString());

        // 2. Run purge scheduler
        purgeScheduler.processPendingPurges();

        // 3. Verify user is gone
        assertThat(userRepository.findById(user.getId())).isEmpty();

        // 4. Verify request is COMPLETED
        Optional<AccountDeletionRequest> requestOpt = deletionRequestRepository.findById(requestId);
        assertThat(requestOpt).isPresent();
        assertThat(requestOpt.get().getStatus()).isEqualTo(DeletionStatus.COMPLETED);
        assertThat(requestOpt.get().getUserId()).isNull(); // Should be null due to ON DELETE SET NULL
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
