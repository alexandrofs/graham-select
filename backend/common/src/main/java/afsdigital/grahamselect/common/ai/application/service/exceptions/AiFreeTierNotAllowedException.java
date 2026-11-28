package afsdigital.grahamselect.common.ai.application.service.exceptions;

/**
 * Thrown when a FREE tier user attempts to consume an LLM call, signaling local deterministic fallback.
 */
public class AiFreeTierNotAllowedException extends RuntimeException {

    public AiFreeTierNotAllowedException(String message) {
        super(message);
    }
}
