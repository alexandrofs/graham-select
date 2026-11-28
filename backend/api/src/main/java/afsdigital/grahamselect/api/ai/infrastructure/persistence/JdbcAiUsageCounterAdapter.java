package afsdigital.grahamselect.api.ai.infrastructure.persistence;

import afsdigital.grahamselect.common.ai.application.port.AiUsageCounterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcAiUsageCounterAdapter implements AiUsageCounterPort {

    private static final String EXISTS_ROW_SQL = """
            SELECT COUNT(*)
            FROM tb_ai_usage_counter
            WHERE user_id = :userId AND usage_date = :usageDate
            """;

    private static final String INSERT_INITIAL_ROW_SQL = """
            INSERT INTO tb_ai_usage_counter (user_id, usage_date, daily_call_count, updated_at)
            VALUES (:userId, :usageDate, 0, :now)
            """;

    private static final String LOCK_MONTHLY_ROWS_SQL = """
            SELECT daily_call_count
            FROM tb_ai_usage_counter
            WHERE user_id = :userId
              AND usage_date >= :monthStart
              AND usage_date <= :usageDate
            FOR UPDATE
            """;

    private static final String ATOMIC_INCREMENT_SQL = """
            UPDATE tb_ai_usage_counter
            SET daily_call_count = daily_call_count + 1,
                updated_at = :now
            WHERE user_id = :userId
              AND usage_date = :usageDate
              AND daily_call_count < :dailyLimit
              AND (
                  SELECT COALESCE(SUM(c.daily_call_count), 0)
                  FROM (
                      SELECT daily_call_count
                      FROM tb_ai_usage_counter
                      WHERE user_id = :userId
                        AND usage_date >= :monthStart
                        AND usage_date <= :usageDate
                  ) c
              ) < :monthlyLimit
            """;

    private static final String DAILY_COUNT_SQL = """
            SELECT COALESCE(SUM(daily_call_count), 0)
            FROM tb_ai_usage_counter
            WHERE user_id = :userId AND usage_date = :usageDate
            """;

    private static final String MONTHLY_COUNT_SQL = """
            SELECT COALESCE(SUM(daily_call_count), 0)
            FROM tb_ai_usage_counter
            WHERE user_id = :userId
              AND usage_date >= :monthStart
              AND usage_date <= :usageDate
            """;

    private static final String DELETE_BY_USER_SQL = """
            DELETE FROM tb_ai_usage_counter
            WHERE user_id = :userId
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public boolean tryConsumeCall(
            Long userId,
            LocalDate usageDateUtc,
            int dailyLimit,
            int monthlyLimit,
            LocalDateTime nowUtc
    ) {
        if (dailyLimit <= 0 || monthlyLimit <= 0) {
            return false;
        }

        ensureRowExists(userId, usageDateUtc, nowUtc);

        LocalDate monthStartUtc = usageDateUtc.withDayOfMonth(1);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("usageDate", usageDateUtc)
                .addValue("monthStart", monthStartUtc)
                .addValue("dailyLimit", dailyLimit)
                .addValue("monthlyLimit", monthlyLimit)
                .addValue("now", nowUtc);

        jdbcTemplate.queryForList(LOCK_MONTHLY_ROWS_SQL, params, Integer.class);
        int affectedRows = jdbcTemplate.update(ATOMIC_INCREMENT_SQL, params);
        return affectedRows == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public int getDailyCallCount(Long userId, LocalDate usageDateUtc) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("usageDate", usageDateUtc);
        Integer count = jdbcTemplate.queryForObject(DAILY_COUNT_SQL, params, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getMonthlyCallCount(Long userId, LocalDate monthStartUtc, LocalDate usageDateUtc) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("monthStart", monthStartUtc)
                .addValue("usageDate", usageDateUtc);
        Integer count = jdbcTemplate.queryForObject(MONTHLY_COUNT_SQL, params, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId);
        jdbcTemplate.update(DELETE_BY_USER_SQL, params);
    }

    private void ensureRowExists(Long userId, LocalDate usageDateUtc, LocalDateTime nowUtc) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("usageDate", usageDateUtc)
                .addValue("now", nowUtc);

        Integer existing = jdbcTemplate.queryForObject(EXISTS_ROW_SQL, params, Integer.class);
        if (existing != null && existing > 0) {
            return;
        }

        try {
            jdbcTemplate.update(INSERT_INITIAL_ROW_SQL, params);
        } catch (DuplicateKeyException concurrentInsert) {
            log.debug("Concurrent AI usage counter row creation for userId={} date={}", userId, usageDateUtc);
        }
    }
}
