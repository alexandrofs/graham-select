package afsdigital.grahamselect.common.ai.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Result produced by {@code AiPortfolioAnalyzerPort}, carrying the 4 validated
 * diagnostic blocks, the generation source, and execution metadata.
 */
public record AiDiagnosticResult(
        AiDiagnosticBlocks blocks,
        AiDiagnosticSource source,
        String modelName,
        String fallbackReason,
        OffsetDateTime generatedAt
) {
    public AiDiagnosticResult {
        Objects.requireNonNull(blocks, "blocks must not be null");
        Objects.requireNonNull(source, "source must not be null");
        generatedAt = generatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : generatedAt;
    }

    public static AiDiagnosticResult fromLlm(AiDiagnosticBlocks blocks, String modelName) {
        return new AiDiagnosticResult(
                blocks,
                AiDiagnosticSource.LLM,
                modelName,
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    public static AiDiagnosticResult fromFallback(AiDiagnosticBlocks blocks, String fallbackReason) {
        return new AiDiagnosticResult(
                blocks,
                AiDiagnosticSource.DETERMINISTIC_FALLBACK,
                "deterministic-fallback",
                fallbackReason,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
