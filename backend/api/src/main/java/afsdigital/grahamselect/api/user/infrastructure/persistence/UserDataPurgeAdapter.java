package afsdigital.grahamselect.api.user.infrastructure.persistence;

import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataPurgeAdapter implements UserDataPurgePort {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void purgeAllUserData(Long userId) {
        if (userId == null) {
            log.warn("Tentativa de expurgo LGPD com userId nulo ignorada.");
            return;
        }

        log.info("Iniciando expurgo definitivo de dados pessoais (LGPD / FR5) para userId={}", userId);

        Set<String> userIdentifiers = new LinkedHashSet<>();
        userIdentifiers.add(String.valueOf(userId));

        Optional<User> userOpt = userRepository.findById(userId);
        userOpt.ifPresent(u -> {
            if (u.getGoogleSub() != null && !u.getGoogleSub().isBlank()) {
                userIdentifiers.add(u.getGoogleSub());
            }
        });

        for (String idStr : userIdentifiers) {
            // 1. Web Push (notificações - Epic 11)
            deleteQuietly("web_push_notification_logs", "user_id", idStr);
            deleteQuietly("web_push_subscriptions", "user_id", idStr);
            deleteQuietly("web_push_preferences", "user_id", idStr);

            // 2. WhatsApp (notificações - Epic 11)
            deleteQuietly("whatsapp_notification_logs", "user_id", idStr);
            deleteQuietly("whatsapp_notification_preferences", "user_id", idStr);

            // 3. Open Finance (Epic 9)
            deleteQuietly("open_finance_sync_logs", "user_id", idStr);
            deleteQuietly("open_finance_connections", "user_id", idStr);

            // 4. Tags de Portfólio (Epic 8)
            deleteQuietly("asset_tags", "user_id", idStr);
            deleteQuietly("portfolio_tags", "user_id", idStr);

            // 5. Histórico de Trades e Auditorias (Epic 2, 3, 7)
            deleteQuietly("manual_trade_audits", "user_id", idStr);
            deleteQuietly("trades", "user_id", idStr);

            // 6. Perfis Familiares (master_user_id)
            deleteQuietly("family_profiles", "master_user_id", idStr);

            // 7. Metas e Recomendações (Epic 4, 5)
            deleteQuietly("financial_goals", "user_id", idStr);
            deleteQuietly("graham_recommendations", "user_id", idStr);
            deleteQuietly("allocation_goals", "user_id", idStr);

            // 8. Ingestão, DLQ e Status B3 (Epic 1, 6)
            deleteQuietly("ingestion_audits", "user_id", idStr);
            deleteQuietly("dlq_messages", "user_id", idStr);
            deleteQuietly("b3_import_status", "user_id", idStr);
        }

        // 9. Diagrama do Cerrado (Epic 3 - user_id BIGINT FK para users.id)
        deleteQuietly("tb_cerrado_asset_grade", "user_id", userId);
        deleteQuietly("tb_cerrado_preferences", "user_id", userId);

        // 10. Kerno AI (Epic 5 - user_id BIGINT FK para users.id)
        deleteQuietly("tb_ai_diagnostic_cache", "user_id", userId);
        deleteQuietly("tb_ai_usage_counter", "user_id", userId);

        log.info("Expurgo definitivo de dados pessoais concluído com sucesso para userId={}", userId);
    }

    private void deleteQuietly(String table, String column, String value) {
        deleteQuietlyInternal(table, column, value);
    }

    private void deleteQuietly(String table, String column, Long value) {
        deleteQuietlyInternal(table, column, value);
    }

    private void deleteQuietlyInternal(String table, String column, Object value) {
        try {
            int rows = jdbcTemplate.update("DELETE FROM " + table + " WHERE " + column + " = ?", value);
            if (rows > 0) {
                log.info("LGPD Purge: removidos {} registros de {}.{}={}", rows, table, column, value);
            }
        } catch (Exception e) {
            log.error("Erro ao expurgar dados na tabela {} para {}={}: {}", table, column, value, e.getMessage(), e);
            throw e;
        }
    }
}
