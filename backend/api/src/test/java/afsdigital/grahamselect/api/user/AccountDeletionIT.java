package afsdigital.grahamselect.api.user;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.user.infrastructure.persistence.jpa.repository.AccountDeletionRequestJpaRepository;
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
    private AccountDeletionRequestJpaRepository deletionRequestJpaRepository;

    @Autowired
    private AccountPurgeScheduler purgeScheduler;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_ai_diagnostic_cache");
        jdbcTemplate.update("DELETE FROM tb_ai_usage_counter");
        jdbcTemplate.update("DELETE FROM tb_cerrado_asset_grade");
        jdbcTemplate.update("DELETE FROM tb_cerrado_preferences");
        jdbcTemplate.update("DELETE FROM web_push_notification_logs");
        jdbcTemplate.update("DELETE FROM web_push_subscriptions");
        jdbcTemplate.update("DELETE FROM web_push_preferences");
        jdbcTemplate.update("DELETE FROM whatsapp_notification_logs");
        jdbcTemplate.update("DELETE FROM whatsapp_notification_preferences");
        jdbcTemplate.update("DELETE FROM financial_goals");
        deletionRequestJpaRepository.deleteAll();
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

        String sub = user.getGoogleSub();
        jdbcTemplate.update(
                "INSERT INTO whatsapp_notification_preferences (id, user_id, phone_number, opted_in, notify_dividends, notify_rebalance_triggers, notify_goal_milestones, rebalance_drift_threshold_pct, created_at, updated_at) " +
                        "VALUES (?, ?, ?, true, true, true, true, 5.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                java.util.UUID.randomUUID().toString(), sub, "+5511999999999"
        );
        jdbcTemplate.update(
                "INSERT INTO whatsapp_notification_logs (id, user_id, event_type, dedup_key, message_preview, provider, status, sent_at) " +
                        "VALUES (?, ?, 'DIVIDEND', 'dedup-wa-test', 'msg', 'mock', 'DELIVERED', CURRENT_TIMESTAMP)",
                java.util.UUID.randomUUID().toString(), sub
        );
        jdbcTemplate.update(
                "INSERT INTO web_push_preferences (id, user_id, opted_in, min_margin_of_safety_pct, only_custody_tickers, created_at, updated_at) " +
                        "VALUES (?, ?, true, 20.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                java.util.UUID.randomUUID().toString(), sub
        );
        jdbcTemplate.update(
                "INSERT INTO web_push_subscriptions (id, user_id, endpoint, endpoint_hash, p256dh_key, auth_key, active, created_at, updated_at) " +
                        "VALUES (?, ?, 'https://push.example.com/test', 'hash-test-1', 'key', 'auth', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                java.util.UUID.randomUUID().toString(), sub
        );
        jdbcTemplate.update(
                "INSERT INTO web_push_notification_logs (id, user_id, event_type, dedup_key, title, message_preview, provider, status, sent_at) " +
                        "VALUES (?, ?, 'GRAHAM_BUY_ZONE', 'dedup-wp-test', 'title', 'msg', 'mock', 'DELIVERED', CURRENT_TIMESTAMP)",
                java.util.UUID.randomUUID().toString(), sub
        );
        jdbcTemplate.update(
                "INSERT INTO financial_goals (id, user_id, goal_type, target_value, monthly_contribution, estimated_years, created_at, updated_at) " +
                        "VALUES (?, ?, 'PATRIMONIO_TOTAL', 10000.00, 500.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                java.util.UUID.randomUUID().toString(), sub
        );
        jdbcTemplate.update(
                "INSERT INTO tb_cerrado_preferences (user_id, target_acoes_br, target_fiis, target_renda_fixa, target_internacional, target_cripto, graham_multiplier, graham_filter_enabled, updated_at) " +
                        "VALUES (?, 35.00, 25.00, 25.00, 14.00, 1.00, 1.30, true, CURRENT_TIMESTAMP)",
                user.getId()
        );
        jdbcTemplate.update(
                "INSERT INTO tb_cerrado_asset_grade (user_id, ticker, macro_class, sub_class, quality_grade, quarantine_corporate_event, updated_at) " +
                        "VALUES (?, 'WEGE3', 'ACOES_BR', 'EQUITY_GROWTH', 9, false, CURRENT_TIMESTAMP)",
                user.getId()
        );
        jdbcTemplate.update(
                "INSERT INTO tb_ai_diagnostic_cache (user_id, portfolio_state_hash, model_version, prompt_version, json_payload, created_at, expires_at) " +
                        "VALUES (?, 'abc123hash', 'gemini-2.5-flash', 'v1', '{\"resumoExecutivo\":\"ok\"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user.getId()
        );
        jdbcTemplate.update(
                "INSERT INTO tb_ai_usage_counter (user_id, usage_date, daily_call_count, updated_at) " +
                        "VALUES (?, CURRENT_DATE, 2, CURRENT_TIMESTAMP)",
                user.getId()
        );

        Integer waPrefsBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM whatsapp_notification_preferences WHERE user_id = ?", Integer.class, sub);
        assertThat(waPrefsBefore).isEqualTo(1);

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

        // 5. Verify LGPD purge removed all notifications and personal data
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM whatsapp_notification_preferences WHERE user_id = ?", Integer.class, sub)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM whatsapp_notification_logs WHERE user_id = ?", Integer.class, sub)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM web_push_preferences WHERE user_id = ?", Integer.class, sub)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM web_push_subscriptions WHERE user_id = ?", Integer.class, sub)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM web_push_notification_logs WHERE user_id = ?", Integer.class, sub)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financial_goals WHERE user_id = ?", Integer.class, sub)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tb_cerrado_preferences WHERE user_id = ?", Integer.class, user.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tb_cerrado_asset_grade WHERE user_id = ?", Integer.class, user.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tb_ai_diagnostic_cache WHERE user_id = ?", Integer.class, user.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tb_ai_usage_counter WHERE user_id = ?", Integer.class, user.getId())).isZero();
    }

    @Test
    @DisplayName("Duplicate deletion request should return 409 Conflict")
    void shouldReturn409WhenDuplicateDeletionRequest() throws Exception {
        User user = createAndMockUser("dup-user", "dup@example.com");
        String token = "dup-token";
        mockJwt(token, user);

        // 1. First request - should succeed
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());

        // 2. Second request - should return 409
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cancel without pending request should return 404")
    void shouldReturn404WhenNoPendingDeletionRequest() throws Exception {
        User user = createAndMockUser("no-pending-user", "nopending@example.com");
        String token = "no-pending-token";
        mockJwt(token, user);

        // Cancel without prior request
        mockMvc.perform(post("/api/v1/users/me/cancel-deletion")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deletion endpoints should not require @RequirePremium - FREE tier can delete")
    void shouldAllowFreeTierToDeleteAccount() throws Exception {
        User user = User.builder()
                .googleSub("free-user-sub")
                .email("free@example.com")
                .fullName("Free User")
                .tier(SubscriptionTier.FREE)
                .build();
        user = userRepository.save(user);

        String token = "free-token";
        mockJwt(token, user);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());
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
