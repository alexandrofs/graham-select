package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GrahamRecommendationsIT extends BaseRepositoryIT {

    private static final String RECOMMENDATIONS_ENDPOINT = "/api/v1/graham-recommendations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GrahamRecommendationJpaRepository recommendationJpaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        recommendationJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldDenyAccessToFreeUsers() throws Exception {
        User user = saveUser("free-sub", SubscriptionTier.FREE, null);
        mockJwtDecoder("free-token", user);

        mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:upgrade-required"));
    }

    @Test
    void shouldAllowPremiumUsersToGetEmptyRecommendations() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldGetRecommendationsOrderedByScoreForPremiumUser() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        // Save dummy recommendations
        recommendationJpaRepository.save(GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getGoogleSub())
                .ticker("PETR4")
                .currentPrice(new BigDecimal("30.0000"))
                .intrinsicValue(new BigDecimal("40.0000"))
                .marginOfSafety(new BigDecimal("0.3300"))
                .currentAllocationPct(new BigDecimal("5.0000"))
                .targetAllocationPct(new BigDecimal("20.0000"))
                .allocationGap(new BigDecimal("15.0000"))
                .recommendationScore(new BigDecimal("6.1980"))
                .generatedAt(LocalDate.now())
                .build());

        recommendationJpaRepository.save(GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getGoogleSub())
                .ticker("VALE3")
                .currentPrice(new BigDecimal("70.0000"))
                .intrinsicValue(new BigDecimal("80.0000"))
                .marginOfSafety(new BigDecimal("0.1400"))
                .currentAllocationPct(new BigDecimal("12.0000"))
                .targetAllocationPct(new BigDecimal("10.0000"))
                .allocationGap(new BigDecimal("-2.0000"))
                .recommendationScore(new BigDecimal("0.0840"))
                .generatedAt(LocalDate.now())
                .build());

        mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].recommendationScore").value(6.198))
                .andExpect(jsonPath("$[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$[1].recommendationScore").value(0.084));
    }

    @Test
    void shouldTriggerCalculationAcceptedForPremiumUser() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
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
