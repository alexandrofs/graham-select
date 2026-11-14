package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.PortfolioTagEntity;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaPortfolioTagRepository;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    @Autowired
    private JpaPortfolioTagRepository portfolioTagRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        allocationGoalJpaRepository.deleteAll();
        portfolioTagRepository.deleteAll();
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

    @Test
    void shouldIsolateAllocationStrategyBetweenGlobalAndTagsWithoutFallback() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);
        UUID tagA = saveTag(user.getGoogleSub(), "Aposentadoria").getId();
        UUID tagB = saveTag(user.getGoogleSub(), "Dividendos").getId();

        String globalBody = """
                [
                  {"goalType": "ASSET_CLASS", "targetKey": "ACOES", "targetPercentage": 60.00},
                  {"goalType": "ASSET_CLASS", "targetKey": "FIIS", "targetPercentage": 40.00},
                  {"goalType": "TICKER", "targetKey": "PETR4", "targetPercentage": 10.00}
                ]
                """;
        mockMvc.perform(post(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(globalBody))
                .andExpect(status().isNoContent());

        // New tag must return empty list without fallback to global strategy
        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .param("tagId", tagA.toString())
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        String tagABody = """
                [
                  {"goalType": "ASSET_CLASS", "targetKey": "ACOES", "targetPercentage": 100.00},
                  {"goalType": "TICKER", "targetKey": "VALE3", "targetPercentage": 25.00}
                ]
                """;
        mockMvc.perform(post(ALLOCATION_STRATEGY_ENDPOINT)
                        .param("tagId", tagA.toString())
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tagABody))
                .andExpect(status().isNoContent());

        // Tag A returns only its own goals
        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .param("tagId", tagA.toString())
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].goalType").value("ASSET_CLASS"))
                .andExpect(jsonPath("$[0].targetKey").value("ACOES"))
                .andExpect(jsonPath("$[0].targetPercentage").value(100.0))
                .andExpect(jsonPath("$[1].goalType").value("TICKER"))
                .andExpect(jsonPath("$[1].targetKey").value("VALE3"))
                .andExpect(jsonPath("$[1].targetPercentage").value(25.0));

        // Global remains untouched
        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].targetKey").value("PETR4"));

        // Tag B remains empty
        mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                        .param("tagId", tagB.toString())
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenTagIdIsUnknownOrForeignOnAllocationStrategyEndpoints() throws Exception {
        User owner = saveUser("owner-sub", SubscriptionTier.PREMIUM, null);
        User other = saveUser("other-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("owner-token", owner);
        UUID foreignTagId = saveTag(other.getGoogleSub(), "Foreign Tag").getId();
        UUID unknownTagId = UUID.randomUUID();
        String validBody = """
                [
                  {"goalType": "ASSET_CLASS", "targetKey": "ACOES", "targetPercentage": 100.00}
                ]
                """;

        for (UUID badTagId : List.of(unknownTagId, foreignTagId)) {
            mockMvc.perform(get(ALLOCATION_STRATEGY_ENDPOINT)
                            .param("tagId", badTagId.toString())
                            .header("Authorization", "Bearer owner-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            mockMvc.perform(post(ALLOCATION_STRATEGY_ENDPOINT)
                            .param("tagId", badTagId.toString())
                            .header("Authorization", "Bearer owner-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    private PortfolioTagEntity saveTag(String userId, String name) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return portfolioTagRepository.save(PortfolioTagEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(name)
                .normalizedName(name.toLowerCase(Locale.ROOT))
                .createdAt(now)
                .updatedAt(now)
                .isNew(true)
                .build());
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
