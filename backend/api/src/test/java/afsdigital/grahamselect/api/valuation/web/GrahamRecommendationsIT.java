package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.PortfolioTagEntity;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaPortfolioTagRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.kafka.ValuationRequestedPublisher;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import afsdigital.grahamselect.common.domain.entities.ValuationRequestedEvent;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Autowired
    private JpaPortfolioTagRepository portfolioTagRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ValuationRequestedPublisher valuationRequestedPublisher;

    @BeforeEach
    void setUp() {
        recommendationJpaRepository.deleteAll();
        portfolioTagRepository.deleteAll();
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
                .epsUsed(new BigDecimal("4.5200"))
                .bvpsUsed(new BigDecimal("31.8000"))
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
                .andExpect(jsonPath("$[0].epsUsed").value(4.52))
                .andExpect(jsonPath("$[0].bvpsUsed").value(31.80))
                .andExpect(jsonPath("$[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$[1].recommendationScore").value(0.084))
                .andExpect(jsonPath("$[1].epsUsed").doesNotExist())
                .andExpect(jsonPath("$[1].bvpsUsed").doesNotExist());
    }

    @Test
    void shouldTriggerCalculationAcceptedForPremiumUser() throws Exception {
        User user = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", user);

        mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(valuationRequestedPublisher).publish(argThat((ValuationRequestedEvent e) ->
                "premium-sub".equals(e.userId()) && e.tagId() == null));
    }

    @Test
    void shouldIsolateRecommendationsByGlobalAndTagScope() throws Exception {
        User user = saveUser("premium-sub-scoped", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("scoped-token", user);
        UUID tagA = saveTag(user.getGoogleSub(), "Aposentadoria").getId();
        UUID tagB = saveTag(user.getGoogleSub(), "Dividendos").getId();

        recommendationJpaRepository.save(buildRecommendation(user.getGoogleSub(), null, AllocationScope.GLOBAL, "PETR4", "6.1980"));
        recommendationJpaRepository.save(buildRecommendation(user.getGoogleSub(), tagA, tagA.toString(), "VALE3", "3.4500"));

        mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                        .header("Authorization", "Bearer scoped-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"));

        mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                        .param("tagId", tagA.toString())
                        .header("Authorization", "Bearer scoped-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("VALE3"));

        mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                        .param("tagId", tagB.toString())
                        .header("Authorization", "Bearer scoped-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldEnforceIndependentCooldownPerScopeKeyAndPublishTagId() throws Exception {
        User user = saveUser("premium-sub-cooldown", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("cooldown-token", user);
        UUID tagA = saveTag(user.getGoogleSub(), "Carteira FIIs").getId();

        mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                        .header("Authorization", "Bearer cooldown-token"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                        .header("Authorization", "Bearer cooldown-token"))
                .andExpect(status().isTooManyRequests());

        // Different scope (tagA) must NOT be blocked by global cooldown
        mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                        .param("tagId", tagA.toString())
                        .header("Authorization", "Bearer cooldown-token"))
                .andExpect(status().isAccepted());

        verify(valuationRequestedPublisher).publish(argThat((ValuationRequestedEvent e) ->
                "premium-sub-cooldown".equals(e.userId()) && tagA.equals(e.tagId())));

        // Repeated trigger on tagA triggers tagA's own cooldown
        mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                        .param("tagId", tagA.toString())
                        .header("Authorization", "Bearer cooldown-token"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldReturn404WhenTagIdIsUnknownOrForeignOnRecommendationEndpoints() throws Exception {
        User owner = saveUser("owner-rec-sub", SubscriptionTier.PREMIUM, null);
        User other = saveUser("other-rec-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("owner-rec-token", owner);
        UUID foreignTagId = saveTag(other.getGoogleSub(), "Foreign Tag").getId();
        UUID unknownTagId = UUID.randomUUID();

        for (UUID badTagId : List.of(unknownTagId, foreignTagId)) {
            mockMvc.perform(get(RECOMMENDATIONS_ENDPOINT)
                            .param("tagId", badTagId.toString())
                            .header("Authorization", "Bearer owner-rec-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            mockMvc.perform(post(RECOMMENDATIONS_ENDPOINT + "/trigger")
                            .param("tagId", badTagId.toString())
                            .header("Authorization", "Bearer owner-rec-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verifyNoInteractions(valuationRequestedPublisher);
    }

    private GrahamRecommendationEntity buildRecommendation(
            String userId, UUID tagId, String scopeKey, String ticker, String score) {
        return GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tagId(tagId)
                .scopeKey(scopeKey)
                .ticker(ticker)
                .currentPrice(new BigDecimal("30.0000"))
                .intrinsicValue(new BigDecimal("40.0000"))
                .marginOfSafety(new BigDecimal("0.3300"))
                .currentAllocationPct(new BigDecimal("5.0000"))
                .targetAllocationPct(new BigDecimal("20.0000"))
                .allocationGap(new BigDecimal("15.0000"))
                .recommendationScore(new BigDecimal(score))
                .generatedAt(LocalDate.now(ZoneOffset.UTC))
                .build();
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
