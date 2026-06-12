package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repositories.AllocationGoalJpaRepository;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AllocationStrategyIT extends BaseRepositoryIT {

    private static final String ALLOCATION_STRATEGY_ENDPOINT = "/api/v1/allocation-strategy";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AllocationGoalJpaRepository allocationGoalJpaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        allocationGoalJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldDenyAccessToFreeUsers() throws Exception {
        User user = saveUser("free-sub", SubscriptionTier.FREE, null);
        mockJwtDecoder("free-token", user);

        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:upgrade-required"));
    }

    @Test
    void shouldAllowPremiumUsersToGetEmptyAllocationStrategy() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldSaveAndRetrieveAllocationStrategyForPremiumUser() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        String requestBody = """
                [
                  {"goalType": "ASSET_CLASS", "targetKey": "ACOES", "targetPercentage": 60.00},
                  {"goalType": "ASSET_CLASS", "targetKey": "FIIS", "targetPercentage": 40.00},
                  {"goalType": "TICKER", "targetKey": "PETR4", "targetPercentage": 10.00}
                ]
                """;

        // Save strategy
        mockMvc.perform(post(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // Verify in DB
        List<AllocationGoalEntity> entities = allocationGoalJpaRepository.findByUserId(user.getGoogleSub());
        assertEquals(3, entities.size());

        // Retrieve strategy
        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].goalType").value("ASSET_CLASS"))
                .andExpect(jsonPath("$[0].targetKey").value("ACOES"))
                .andExpect(jsonPath("$[0].targetPercentage").value(60.0))
                .andExpect(jsonPath("$[1].goalType").value("ASSET_CLASS"))
                .andExpect(jsonPath("$[1].targetKey").value("FIIS"))
                .andExpect(jsonPath("$[1].targetPercentage").value(40.0))
                .andExpect(jsonPath("$[2].goalType").value("TICKER"))
                .andExpect(jsonPath("$[2].targetKey").value("PETR4"))
                .andExpect(jsonPath("$[2].targetPercentage").value(10.0));
    }

    @Test
    void shouldReturnBadRequestWhenAssetClassSumIsNot100Percent() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        String requestBody = """
                [
                  {"goalType": "ASSET_CLASS", "targetKey": "ACOES", "targetPercentage": 50.00},
                  {"goalType": "ASSET_CLASS", "targetKey": "FIIS", "targetPercentage": 40.00}
                ]
                """;

        mockMvc.perform(post(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Allocation Goal Validation Error"))
                .andExpect(jsonPath("$.detail").value("A soma das metas de classe deve ser exatamente 100%. Soma atual: 90.00%"));
    }

    private User saveUser(String googleSub, SubscriptionTier tier, LocalDateTime trialEndsAt) {
        return userRepository.save(User.builder()
                .googleSub(googleSub)
                .email(googleSub + "@example.com")
                .fullName("Test User " + googleSub)
                .tier(tier)
                .trialEndsAt(trialEndsAt)
                .build());
    }

    private void mockJwtDecoder(String tokenValue, User user) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .claim("sub", user.getGoogleSub())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);
    }
}
