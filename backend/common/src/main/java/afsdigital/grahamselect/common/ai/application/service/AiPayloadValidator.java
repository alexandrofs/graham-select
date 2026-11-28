package afsdigital.grahamselect.common.ai.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Defensive pre-flight validator executed on the serialized JSON payload before any
 * external AI network call. Enforces the 4,000-token budget and blocks residual PII
 * (CPF, CNPJ, email, phone) or currency markers (R$, US$, BRL) without false positives
 * on ISO-8601 dates or 64-char SHA-256 hex hashes.
 */
public class AiPayloadValidator {

    public static final int MAX_ESTIMATED_TOKENS = 4_000;

    private static final Logger log = LoggerFactory.getLogger(AiPayloadValidator.class);

    private static final List<PatternRule> FORBIDDEN_PATTERNS = List.of(
            new PatternRule(
                    "CPF_PATTERN",
                    Pattern.compile("(?<!\\d)\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}(?!\\d)|(?<![0-9a-fA-F])\\d{11}(?![0-9a-fA-F])")
            ),
            new PatternRule(
                    "CNPJ_PATTERN",
                    Pattern.compile("(?<!\\d)\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}(?!\\d)|(?<![0-9a-fA-F])\\d{14}(?![0-9a-fA-F])")
            ),
            new PatternRule(
                    "EMAIL_PATTERN",
                    Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
            ),
            new PatternRule(
                    "PHONE_PATTERN",
                    Pattern.compile("(?<![0-9a-fA-F])(?:\\+?55\\s?)?(?:\\(?\\d{2}\\)?\\s?)(?:9?\\d{4}[-.\\s]?\\d{4})(?![0-9a-fA-F])")
            ),
            new PatternRule(
                    "CURRENCY_PATTERN",
                    Pattern.compile("(?i)(?:R\\$|US\\$|€|\\b(?:BRL|USD|EUR)\\b)")
            ),
            new PatternRule(
                    "RAW_SENSITIVE_FIELD_PATTERN",
                    Pattern.compile("\"(?:ownerName|brokerName|accountLabel|marketValueBrl|averagePriceBrl|totalNetWorthBrl|quantity|userId)\"\\s*:")
            )
    );

    public void validate(String serializedPayloadJson) {
        if (serializedPayloadJson == null || serializedPayloadJson.isBlank()) {
            log.error("AI payload validation failed: violation=BLANK_PAYLOAD");
            throw new AiPayloadValidationException("BLANK_PAYLOAD", "Serialized AI payload must not be blank");
        }

        int utf8Bytes = serializedPayloadJson.getBytes(StandardCharsets.UTF_8).length;
        int estimatedTokens = (int) Math.ceil(utf8Bytes / 4.0);
        if (estimatedTokens > MAX_ESTIMATED_TOKENS) {
            log.error(
                    "AI payload validation failed: violation=TOKEN_LIMIT_EXCEEDED estimatedTokens={} maxTokens={}",
                    estimatedTokens,
                    MAX_ESTIMATED_TOKENS
            );
            throw new AiPayloadValidationException(
                    "TOKEN_LIMIT_EXCEEDED",
                    "Estimated token count " + estimatedTokens + " exceeds limit of " + MAX_ESTIMATED_TOKENS
            );
        }

        for (PatternRule rule : FORBIDDEN_PATTERNS) {
            if (rule.pattern().matcher(serializedPayloadJson).find()) {
                log.error("AI payload validation failed: violation={}", rule.code());
                throw new AiPayloadValidationException(
                        rule.code(),
                        "Serialized AI payload contains forbidden pattern: " + rule.code()
                );
            }
        }
    }

    private record PatternRule(String code, Pattern pattern) {
    }
}
