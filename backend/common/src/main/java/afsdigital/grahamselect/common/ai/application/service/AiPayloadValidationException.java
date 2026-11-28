package afsdigital.grahamselect.common.ai.application.service;

/**
 * Thrown by {@link AiPayloadValidator} when a serialized AI payload violates
 * the token budget or contains residual PII or monetary currency patterns.
 */
public class AiPayloadValidationException extends RuntimeException {

    private final String violationCode;

    public AiPayloadValidationException(String violationCode, String message) {
        super(message);
        this.violationCode = violationCode;
    }

    public String getViolationCode() {
        return violationCode;
    }
}
