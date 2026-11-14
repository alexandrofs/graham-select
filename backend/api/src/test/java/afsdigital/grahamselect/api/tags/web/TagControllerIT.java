package afsdigital.grahamselect.api.tags.web;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaAssetTagRepository;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaPortfolioTagRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repositories.AllocationGoalJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TagControllerIT extends BaseRepositoryIT {

    private static final String ENDPOINT = "/api/v1/tags";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JpaPortfolioTagRepository portfolioTagRepository;

    @Autowired
    private JpaAssetTagRepository assetTagRepository;

    @Autowired
    private JpaTradeRepository tradeRepository;

    @Autowired
    private AllocationGoalJpaRepository allocationGoalJpaRepository;

    @Autowired
    private GrahamRecommendationJpaRepository recommendationJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        allocationGoalJpaRepository.deleteAll();
        recommendationJpaRepository.deleteAll();
        assetTagRepository.deleteAll();
        portfolioTagRepository.deleteAll();
        tradeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowFullTagLifecycleAndProtectTradesForFreeTierUser() throws Exception {
        User freeUser = saveUser("free-user-sub", SubscriptionTier.FREE);
        mockJwtDecoder("free-token", freeUser);

        TradeEntity existingTrade = tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(freeUser.getGoogleSub())
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.of(2026, 5, 10))
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("35.0000"))
                .broker("XP")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        // 1. Create tags
        MvcResult createResult1 = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aposentadoria\",\"color\":\"#10b981\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Aposentadoria"))
                .andExpect(jsonPath("$.data.color").value("#10B981"))
                .andExpect(jsonPath("$.data.assetCount").value(0))
                .andReturn();
        String tag1Id = extractId(createResult1);

        MvcResult createResult2 = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Longo Prazo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.color").value("#6366F1"))
                .andReturn();
        String tag2Id = extractId(createResult2);

        // 2. List tags
        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer free-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.total").value(2));

        // 3. Duplicate case-insensitive -> 409 Conflict (ProblemDetail)
        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  APOSENTADORIA  \"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Duplicate Tag"));

        // 4. Associate multiple tags to PETR4 via PUT /assets/{ticker}
        mockMvc.perform(put(ENDPOINT + "/assets/petr4")
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tagIds", List.of(tag1Id, tag2Id, tag1Id)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("PETR4"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andExpect(jsonPath("$.data.tags[*].name", containsInAnyOrder("Aposentadoria", "Longo Prazo")));

        // 5. Assign fixed-income tickers with space and hyphen via POST /{tagId}/assets/{ticker}
        mockMvc.perform(post(ENDPOINT + "/{tagId}/assets/{ticker}", tag1Id, "cdb banco x")
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tagId").value(tag1Id))
                .andExpect(jsonPath("$.data.ticker").value("CDB BANCO X"));

        mockMvc.perform(post(ENDPOINT + "/{tagId}/assets/{ticker}", tag2Id, "lci-2026")
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ticker").value("LCI-2026"));

        // 6. Query tags by asset ticker
        mockMvc.perform(get(ENDPOINT + "/assets/{ticker}", "CDB BANCO X")
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("CDB BANCO X"))
                .andExpect(jsonPath("$.data.tags", hasSize(1)))
                .andExpect(jsonPath("$.data.tags[0].id").value(tag1Id));

        // 7. Rename tag (and update color), verifying linked assets remain reflected
        mockMvc.perform(put(ENDPOINT + "/" + tag1Id)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Previdência\",\"color\":\"#8B5CF6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Previdência"))
                .andExpect(jsonPath("$.data.color").value("#8B5CF6"))
                .andExpect(jsonPath("$.data.assetCount").value(2))
                .andExpect(jsonPath("$.data.tickers", containsInAnyOrder("CDB BANCO X", "PETR4")));

        // Renaming to an existing tag name belonging to another tag -> 409 Conflict
        mockMvc.perform(put(ENDPOINT + "/" + tag1Id)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Longo Prazo\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Self-rename (same tag, changing only capitalization/color) should succeed without 409
        mockMvc.perform(put(ENDPOINT + "/" + tag1Id)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"previdência\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("previdência"))
                .andExpect(jsonPath("$.data.color").value("#8B5CF6"));

        // Replace PETR4 tags via PUT (diff: keeps tag1, removes tag2; then restores [tag1, tag2])
        mockMvc.perform(put(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tagIds", List.of(tag1Id)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", hasSize(1)))
                .andExpect(jsonPath("$.data.tags[0].id").value(tag1Id));

        mockMvc.perform(put(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tagIds", List.of(tag1Id, tag2Id)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", hasSize(2)));

        // 8. Unassign single tag from PETR4
        mockMvc.perform(delete(ENDPOINT + "/{tagId}/assets/{ticker}", tag2Id, "PETR4")
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", hasSize(1)))
                .andExpect(jsonPath("$.data.tags[0].id").value(tag1Id));

        // 9. Delete tag -> 204 and trades table remains untouched
        mockMvc.perform(delete(ENDPOINT + "/" + tag1Id)
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", hasSize(0)));

        assertThat(tradeRepository.findAllByUserId(freeUser.getGoogleSub()))
                .hasSize(1)
                .extracting(TradeEntity::getId)
                .containsExactly(existingTrade.getId());
    }

    @Test
    void shouldReturn400ForInvalidInputsMalformedUuidAndMalformedJson() throws Exception {
        User user = saveUser("validation-user", SubscriptionTier.FREE);
        mockJwtDecoder("val-token", user);

        // Blank name -> 400
        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer val-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Invalid hex color -> 400
        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer val-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dividendos\",\"color\":\"not-hex\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Malformed UUID in path -> 400
        mockMvc.perform(put(ENDPOINT + "/not-a-uuid")
                        .header("Authorization", "Bearer val-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dividendos\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid Parameter"));

        // Malformed JSON -> 400
        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer val-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed JSON Request"));

        // Invalid ticker with path traversal -> 400
        mockMvc.perform(get(ENDPOINT + "/assets/{ticker}", "PETR..4")
                        .header("Authorization", "Bearer val-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Null tagIds -> 400
        mockMvc.perform(put(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer val-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldEnforceStrictMultiTenantIsolationBetweenUsers() throws Exception {
        User userA = saveUser("user-a-sub", SubscriptionTier.FREE);
        User userB = saveUser("user-b-sub", SubscriptionTier.FREE);
        mockJwtDecoder("token-a", userA);
        mockJwtDecoder("token-b", userB);

        // Both users can create a tag with the exact same name
        MvcResult createA = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer token-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dividendos\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tagAId = extractId(createA);

        MvcResult createB = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer token-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dividendos\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tagBId = extractId(createB);

        assertThat(tagAId).isNotEqualTo(tagBId);

        // User B cannot rename, delete, or assign assets using User A's tagId -> 404 Not Found
        mockMvc.perform(put(ENDPOINT + "/" + tagAId)
                        .header("Authorization", "Bearer token-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hack\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(delete(ENDPOINT + "/" + tagAId)
                        .header("Authorization", "Bearer token-b"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post(ENDPOINT + "/" + tagAId + "/assets/PETR4")
                        .header("Authorization", "Bearer token-b"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(put(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer token-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tagIds", List.of(tagAId)))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Assign PETR4 for User A, verify User B still gets 404 on DELETE and empty tags on GET /assets/PETR4
        mockMvc.perform(post(ENDPOINT + "/" + tagAId + "/assets/PETR4")
                        .header("Authorization", "Bearer token-a"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(ENDPOINT + "/" + tagAId + "/assets/PETR4")
                        .header("Authorization", "Bearer token-b"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer token-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", hasSize(0)));

        // User A's list contains only User A's tag
        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer token-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(tagAId));
    }

    @Test
    void shouldAcceptPaddedMaxLengthNameAndColorAndRejectNewlinesInName() throws Exception {
        User user = saveUser("padded-tag-sub", SubscriptionTier.FREE);
        mockJwtDecoder("padded-token", user);
        String exact50 = "A".repeat(50);

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer padded-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "  " + exact50 + "  ",
                                "color", " #10b981 "
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(exact50))
                .andExpect(jsonPath("$.data.color").value("#10B981"));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer padded-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Tag\nInvalida"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldAssignIdempotentlyClearWithEmptyListAndHandleUnknownOrNullTagIds() throws Exception {
        User user = saveUser("idempotent-tag-sub", SubscriptionTier.FREE);
        mockJwtDecoder("idem-token", user);

        MvcResult created = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer idem-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renda\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tagId = extractId(created);

        // Repeated POST assign is idempotent and keeps assetCount = 1
        mockMvc.perform(post(ENDPOINT + "/" + tagId + "/assets/PETR4")
                        .header("Authorization", "Bearer idem-token"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(ENDPOINT + "/" + tagId + "/assets/petr4")
                        .header("Authorization", "Bearer idem-token"))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer idem-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].assetCount").value(1));

        // PUT with [] clears all associations
        mockMvc.perform(put(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer idem-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", hasSize(0)));

        // PUT with [null] returns 400
        mockMvc.perform(put(ENDPOINT + "/assets/PETR4")
                        .header("Authorization", "Bearer idem-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Unknown tagId on POST/DELETE asset endpoints returns 404
        UUID unknownTagId = UUID.randomUUID();
        mockMvc.perform(post(ENDPOINT + "/" + unknownTagId + "/assets/PETR4")
                        .header("Authorization", "Bearer idem-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(delete(ENDPOINT + "/" + unknownTagId + "/assets/PETR4")
                        .header("Authorization", "Bearer idem-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldCascadeDeleteScopedAllocationGoalsAndRecommendationsWhilePreservingGlobalRows() throws Exception {
        User user = saveUser("cascade-tag-sub", SubscriptionTier.PREMIUM);
        mockJwtDecoder("cascade-token", user);

        MvcResult created = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer cascade-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aposentadoria\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID tagId = UUID.fromString(extractId(created));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        allocationGoalJpaRepository.save(AllocationGoalEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getGoogleSub())
                .tagId(null)
                .scopeKey(AllocationScope.GLOBAL)
                .goalType("ASSET_CLASS")
                .targetKey("ACOES")
                .targetPercentage(new BigDecimal("100.0000"))
                .createdAt(now)
                .updatedAt(now)
                .build());
        allocationGoalJpaRepository.save(AllocationGoalEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getGoogleSub())
                .tagId(tagId)
                .scopeKey(tagId.toString())
                .goalType("ASSET_CLASS")
                .targetKey("FIIS")
                .targetPercentage(new BigDecimal("100.0000"))
                .createdAt(now)
                .updatedAt(now)
                .build());

        recommendationJpaRepository.save(GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getGoogleSub())
                .tagId(null)
                .scopeKey(AllocationScope.GLOBAL)
                .ticker("PETR4")
                .currentPrice(new BigDecimal("30.0000"))
                .intrinsicValue(new BigDecimal("40.0000"))
                .marginOfSafety(new BigDecimal("0.3300"))
                .currentAllocationPct(new BigDecimal("5.0000"))
                .targetAllocationPct(new BigDecimal("20.0000"))
                .allocationGap(new BigDecimal("15.0000"))
                .recommendationScore(new BigDecimal("6.1980"))
                .generatedAt(LocalDate.now(ZoneOffset.UTC))
                .build());
        recommendationJpaRepository.save(GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getGoogleSub())
                .tagId(tagId)
                .scopeKey(tagId.toString())
                .ticker("VALE3")
                .currentPrice(new BigDecimal("70.0000"))
                .intrinsicValue(new BigDecimal("80.0000"))
                .marginOfSafety(new BigDecimal("0.1400"))
                .currentAllocationPct(new BigDecimal("5.0000"))
                .targetAllocationPct(new BigDecimal("20.0000"))
                .allocationGap(new BigDecimal("15.0000"))
                .recommendationScore(new BigDecimal("3.1000"))
                .generatedAt(LocalDate.now(ZoneOffset.UTC))
                .build());

        mockMvc.perform(delete(ENDPOINT + "/" + tagId)
                        .header("Authorization", "Bearer cascade-token"))
                .andExpect(status().isNoContent());

        List<AllocationGoalEntity> remainingGoals = allocationGoalJpaRepository.findAll();
        assertThat(remainingGoals).hasSize(1);
        assertThat(remainingGoals.get(0).getScopeKey()).isEqualTo(AllocationScope.GLOBAL);

        List<GrahamRecommendationEntity> remainingRecs = recommendationJpaRepository.findAll();
        assertThat(remainingRecs).hasSize(1);
        assertThat(remainingRecs.get(0).getScopeKey()).isEqualTo(AllocationScope.GLOBAL);
    }

    private String extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("id").asText();
    }

    private User saveUser(String googleSub, SubscriptionTier tier) {
        return userRepository.save(User.builder()
                .googleSub(googleSub)
                .email(googleSub + "@example.com")
                .fullName("Test User " + googleSub)
                .tier(tier)
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
