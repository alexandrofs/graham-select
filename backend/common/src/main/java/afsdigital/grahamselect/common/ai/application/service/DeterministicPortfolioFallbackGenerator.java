package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticBlocks;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticResult;
import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Quantitative local fallback generator used when Vertex AI is unavailable,
 * times out, or produces output rejected by {@link PostGenerationValidator}.
 * Produces 4 strictly educational, CVM-compliant pt-BR blocks without PII or BRL values.
 */
public class DeterministicPortfolioFallbackGenerator {

    private static final BigDecimal HIGH_HHI_THRESHOLD = new BigDecimal("0.25");
    private static final BigDecimal MODERATE_HHI_THRESHOLD = new BigDecimal("0.15");

    public AiDiagnosticResult generate(SanitizedPortfolioSnapshot snapshot, String fallbackReason) {
        if (snapshot == null || snapshot.positions().isEmpty()) {
            AiDiagnosticBlocks emptyBlocks = new AiDiagnosticBlocks(
                    "Carteira sem posições ativas consolidadas no momento. Adicione ativos e defina suas metas no Diagrama do Cerrado para acompanhar os indicadores percentuais de alocação.",
                    "A estrutura está pronta para consolidar pesos percentuais, notas de qualidade e índice de concentração HHI assim que os primeiros ativos forem registrados.",
                    "Sem ativos alocados no snapshot atual, não há exposição a risco de concentração ou eventos corporativos em quarentena.",
                    "Defina seus percentuais-alvo por macroclasse para que o plano de aporte priorize automaticamente as classes com maior desvio positivo em relação à meta."
            );
            return AiDiagnosticResult.fromFallback(emptyBlocks, fallbackReason);
        }

        String resumo = buildResumoExecutivo(snapshot);
        String fortes = buildPontosFortes(snapshot);
        String alertas = buildAlertas(snapshot);
        String racional = buildRacionalAporte(snapshot);

        return AiDiagnosticResult.fromFallback(
                new AiDiagnosticBlocks(resumo, fortes, alertas, racional),
                fallbackReason
        );
    }

    private String buildResumoExecutivo(SanitizedPortfolioSnapshot snapshot) {
        BigDecimal hhi = snapshot.hhiIndex().setScale(4, RoundingMode.HALF_EVEN);
        String hhiLabel = classifyHhi(hhi);
        SanitizedAssetPosition topPosition = snapshot.positions().stream()
                .max(Comparator.comparing(SanitizedAssetPosition::weightPercent))
                .orElseThrow();

        return String.format(
                Locale.ROOT,
                "Análise quantitativa local da carteira: o índice de concentração HHI está em %s (%s). "
                        + "A maior participação individual pertence a %s com %s%% do portfólio.",
                formatDecimal(hhi),
                hhiLabel,
                topPosition.canonicalTicker(),
                formatPercent(topPosition.weightPercent())
        );
    }

    private String buildPontosFortes(SanitizedPortfolioSnapshot snapshot) {
        List<SanitizedAssetPosition> highGrade = snapshot.positions().stream()
                .filter(p -> p.qualityGrade() != null && p.qualityGrade() >= 7 && !p.quarantineCorporateEvent())
                .sorted(Comparator.comparing(SanitizedAssetPosition::qualityGrade).reversed()
                        .thenComparing(SanitizedAssetPosition::weightPercent, Comparator.reverseOrder()))
                .limit(3)
                .toList();

        if (!highGrade.isEmpty()) {
            StringBuilder sb = new StringBuilder("Destaques qualitativos no snapshot: ");
            for (int i = 0; i < highGrade.size(); i++) {
                SanitizedAssetPosition pos = highGrade.get(i);
                if (i > 0) {
                    sb.append("; ");
                }
                sb.append(pos.canonicalTicker())
                        .append(" (peso ")
                        .append(formatPercent(pos.weightPercent()))
                        .append("%, nota ")
                        .append(pos.qualityGrade())
                        .append("/10)");
                if (pos.grahamMarginPercent() != null) {
                    sb.append(" com margem de Graham de ")
                            .append(formatPercent(pos.grahamMarginPercent()))
                            .append("%");
                }
            }
            sb.append(".");
            return sb.toString();
        }

        SanitizedAssetPosition largest = snapshot.positions().stream()
                .max(Comparator.comparing(SanitizedAssetPosition::weightPercent))
                .orElseThrow();
        return String.format(
                Locale.ROOT,
                "A carteira mantém alocação estruturada em torno de %s (%s%%), permitindo acompanhamento percentual contínuo das metas por macroclasse.",
                largest.canonicalTicker(),
                formatPercent(largest.weightPercent())
        );
    }

    private String buildAlertas(SanitizedPortfolioSnapshot snapshot) {
        List<String> alerts = new java.util.ArrayList<>();

        BigDecimal hhi = snapshot.hhiIndex();
        if (hhi.compareTo(HIGH_HHI_THRESHOLD) >= 0) {
            alerts.add("Concentração elevada indicada pelo HHI de " + formatDecimal(hhi.setScale(4, RoundingMode.HALF_EVEN)) + ".");
        }

        List<String> quarantined = snapshot.positions().stream()
                .filter(SanitizedAssetPosition::quarantineCorporateEvent)
                .map(SanitizedAssetPosition::canonicalTicker)
                .toList();
        if (!quarantined.isEmpty()) {
            alerts.add("Ativos em quarentena por evento corporativo que exigem monitoramento: " + String.join(", ", quarantined) + ".");
        }

        List<MacroClassAllocationSnapshot> overAllocated = snapshot.macroClassAllocations().stream()
                .filter(m -> m.deviationPercent().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(MacroClassAllocationSnapshot::deviationPercent).reversed())
                .toList();
        if (!overAllocated.isEmpty()) {
            MacroClassAllocationSnapshot topOver = overAllocated.get(0);
            alerts.add(String.format(
                    Locale.ROOT,
                    "A macroclasse %s está acima da meta (%s%% atual vs %s%% alvo, desvio de %s%%).",
                    topOver.macroClass().name(),
                    formatPercent(topOver.currentPercent()),
                    formatPercent(topOver.targetPercent()),
                    formatPercent(topOver.deviationPercent())
            ));
        }

        if (alerts.isEmpty()) {
            return "Nenhum alerta crítico de concentração (HHI "
                    + formatDecimal(hhi.setScale(4, RoundingMode.HALF_EVEN))
                    + "), eventos corporativos em quarentena ou sobrealocação relevante no snapshot atual.";
        }
        return String.join(" ", alerts);
    }

    private String buildRacionalAporte(SanitizedPortfolioSnapshot snapshot) {
        List<MacroClassAllocationSnapshot> underAllocated = snapshot.macroClassAllocations().stream()
                .filter(m -> m.deviationPercent().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(MacroClassAllocationSnapshot::deviationPercent))
                .toList();

        if (!underAllocated.isEmpty()) {
            MacroClassAllocationSnapshot topUnder = underAllocated.get(0);
            return String.format(
                    Locale.ROOT,
                    "Pelo critério de rebalanceamento percentual, a macroclasse %s apresenta o maior déficit em relação à meta (%s%% atual vs %s%% alvo, desvio de %s%%), "
                            + "sendo a principal referência para novos aportes conforme suas regras individuais.",
                    topUnder.macroClass().name(),
                    formatPercent(topUnder.currentPercent()),
                    formatPercent(topUnder.targetPercent()),
                    formatPercent(topUnder.deviationPercent())
            );
        }

        return "As macroclasses estão alinhadas ou acima das metas configuradas no Diagrama do Cerrado; "
                + "novos aportes podem seguir a distribuição proporcional definida no planejamento pessoal.";
    }

    private static String classifyHhi(BigDecimal hhi) {
        if (hhi.compareTo(HIGH_HHI_THRESHOLD) >= 0) {
            return "concentração elevada";
        }
        if (hhi.compareTo(MODERATE_HHI_THRESHOLD) >= 0) {
            return "concentração moderada";
        }
        return "boa diversificação";
    }

    private static String formatPercent(BigDecimal value) {
        return formatDecimal(value.setScale(1, RoundingMode.HALF_EVEN));
    }

    private static String formatDecimal(BigDecimal value) {
        return value.toPlainString().replace('.', ',');
    }
}
