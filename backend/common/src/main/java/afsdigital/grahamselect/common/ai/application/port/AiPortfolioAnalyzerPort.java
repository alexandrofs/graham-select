package afsdigital.grahamselect.common.ai.application.port;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticResult;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;

/**
 * Outbound port for generating a 4-block portfolio diagnostic from an
 * LGPD-sanitized portfolio snapshot.
 */
public interface AiPortfolioAnalyzerPort {

    AiDiagnosticResult analyze(SanitizedPortfolioSnapshot snapshot);
}
