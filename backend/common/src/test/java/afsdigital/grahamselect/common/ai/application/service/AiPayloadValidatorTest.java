package afsdigital.grahamselect.common.ai.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiPayloadValidatorTest {

    private final AiPayloadValidator validator = new AiPayloadValidator();

    @Test
    void shouldAllowValidPayloadWithIsoDatesAndSha256HashContainingDigits() {
        String validJson = """
                {
                  "generatedAt": "2026-11-28T03:16:13Z",
                  "snapshotDate": "2026-11-28",
                  "portfolioStateHash": "a12345678901234bcdef0123456789abcdef0123456789abcdef0123456789ab",
                  "hhiIndex": 0.1750,
                  "positions": [
                    {"canonicalTicker": "PETR4", "weightPercent": 20.00, "qualityGrade": 8}
                  ]
                }
                """;

        assertThatCode(() -> validator.validate(validJson)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowExact4000TokensAndReject4001Tokens() {
        String exact4000Tokens = "\"" + "a".repeat(15_998) + "\"";
        assertThatCode(() -> validator.validate(exact4000Tokens)).doesNotThrowAnyException();

        String over4000Tokens = "\"" + "a".repeat(15_999) + "\"";
        assertThatThrownBy(() -> validator.validate(over4000Tokens))
                .isInstanceOf(AiPayloadValidationException.class)
                .hasMessageContaining("4001");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"cpf\":\"123.456.789-00\"}",
            "{\"cpf\":\"12345678900\"}",
            "{\"cnpj\":\"12.345.678/0001-99\"}",
            "{\"cnpj\":\"12345678000199\"}",
            "{\"email\":\"investidor@kerno.com.br\"}",
            "{\"phone\":\"+55 11 98765-4321\"}",
            "{\"phone\":\"55 11 98765-4321\"}",
            "{\"phone\":\"(11) 3456-7890\"}",
            "{\"valor\":\"R$ 50.000,00\"}",
            "{\"valor\":\"US$ 1,200.00\"}",
            "{\"valor\":\"€ 500.00\"}",
            "{\"currency\":\"BRL\"}",
            "{\"currency\":\"USD\"}",
            "{\"currency\":\"EUR\"}",
            "{\"ownerName\":\"Joao\"}",
            "{\"brokerName\":\"XP\"}",
            "{\"accountLabel\":\"Principal\"}",
            "{\"marketValueBrl\":1000.00}",
            "{\"averagePriceBrl\":10.00}",
            "{\"totalNetWorthBrl\":50000.00}",
            "{\"quantity\":100}",
            "{\"userId\":42}"
    })
    void shouldBlockResidualPiiAndCurrencyPatterns(String payloadWithLeak) {
        assertThatThrownBy(() -> validator.validate(payloadWithLeak))
                .isInstanceOf(AiPayloadValidationException.class);
    }

    @Test
    void shouldCountUtf8MultibyteBytesForTokenCeiling() {
        // 'ç' is 2 bytes in UTF-8: 8000 * 2 = 16000 bytes -> 4000 tokens (allowed)
        String exact4000Multibyte = "ç".repeat(8_000);
        assertThatCode(() -> validator.validate(exact4000Multibyte)).doesNotThrowAnyException();

        // 8000 * 2 + 1 = 16001 bytes -> ceil(16001 / 4.0) = 4001 tokens (rejected)
        String over4000Multibyte = "ç".repeat(8_000) + "a";
        assertThatThrownBy(() -> validator.validate(over4000Multibyte))
                .isInstanceOf(AiPayloadValidationException.class)
                .hasMessageContaining("4001");
    }

    @Test
    void shouldRejectNullAndBlankPayload() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(AiPayloadValidationException.class);
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(AiPayloadValidationException.class);
    }
}
