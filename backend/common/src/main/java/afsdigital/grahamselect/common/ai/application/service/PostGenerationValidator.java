package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticBlocks;
import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic post-generation validator for Kerno AI diagnostic blocks.
 * Enforces:
 * 1) All 4 blocks non-blank;
 * 2) Zero invented tickers outside the input snapshot (excluding known market acronyms);
 * 3) Every cited number anchored to snapshot values within ±0.1 (±0.01 for hhiIndex);
 * 4) CVM 19/20 lexical filter blocking imperative buy/sell or guaranteed return terms.
 */
public class PostGenerationValidator {

    private static final double DEFAULT_TOLERANCE = 0.1 + 1e-9;
    private static final double HHI_TOLERANCE = 0.01 + 1e-9;

    private static final Pattern CVM_IMPERATIVE_PATTERN = Pattern.compile(
            "\\b(?:comprar?|compre|adquira|vender?|venda|lucro\\s+garantido|retorno\\s+garantido|rentabilidade\\s+garantida|ganho\\s+garantido|recomendamos|recomendo\\s+(?:a\\s+)?(?:compra|venda))\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TICKER_CANDIDATE_PATTERN = Pattern.compile(
            "\\b(?:[A-Za-z_]+_MANUAL_\\d+|TESOURO_[A-Za-z0-9_]+|[A-Za-z]{4}\\d{1,2}F?|[A-Z]{2,5}\\.[A-Z]|[A-Z]{2,5})\\b"
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_])(?:(?<=^|[\\s(])-)?\\d+(?:[.,]\\d+)?(?![A-Za-z0-9_])"
    );

    private static final Set<String> EXCLUDED_ACRONYMS = buildExcludedAcronyms();

    public ValidationResult validate(AiDiagnosticBlocks blocks, SanitizedPortfolioSnapshot snapshot) {
        if (snapshot == null) {
            return ValidationResult.rejected("MISSING_SNAPSHOT", "SanitizedPortfolioSnapshot must not be null");
        }
        if (blocks == null || !blocks.hasAllNonBlankBlocks()) {
            return ValidationResult.rejected("INCOMPLETE_BLOCKS", "All 4 diagnostic blocks must be non-blank");
        }

        String combinedText = String.join(
                "\n",
                blocks.resumoExecutivo(),
                blocks.pontosFortes(),
                blocks.alertasDeRiscoEConcentracao(),
                blocks.racionalDoPlanoDeAporte()
        );

        Matcher cvmMatcher = CVM_IMPERATIVE_PATTERN.matcher(combinedText);
        if (cvmMatcher.find()) {
            return ValidationResult.rejected(
                    "CVM_REGULATORY_VIOLATION",
                    "CVM 19/20 lexical filter triggered by forbidden term"
            );
        }

        Set<String> allowedTickers = new HashSet<>();
        for (SanitizedAssetPosition position : snapshot.positions()) {
            allowedTickers.add(position.canonicalTicker().trim().toUpperCase(Locale.ROOT));
        }

        String textWithoutRatios = combinedText
                .replaceAll("(?i)\\bP/VP\\b", " ")
                .replaceAll("(?i)\\bP/L\\b", " ");

        Matcher tickerMatcher = TICKER_CANDIDATE_PATTERN.matcher(textWithoutRatios);
        while (tickerMatcher.find()) {
            String token = tickerMatcher.group().toUpperCase(Locale.ROOT);
            if (!allowedTickers.contains(token) && !EXCLUDED_ACRONYMS.contains(token)) {
                return ValidationResult.rejected(
                        "HALLUCINATED_TICKER",
                        "Invented or unanchored ticker token detected: " + token
                );
            }
        }

        String maskedText = maskAllowedTokensForNumberCheck(textWithoutRatios, allowedTickers)
                .replaceAll("(?<=\\d)/10\\b", "");

        List<Double> standardAnchors = collectStandardAnchors(snapshot);
        double hhiAnchor = snapshot.hhiIndex().doubleValue();

        Matcher numberMatcher = NUMBER_PATTERN.matcher(maskedText);
        while (numberMatcher.find()) {
            String rawNumber = numberMatcher.group().replace(',', '.');
            double cited;
            try {
                cited = Double.parseDouble(rawNumber);
            } catch (NumberFormatException ex) {
                return ValidationResult.rejected("INVALID_NUMBER_FORMAT", "Unparseable number in output");
            }
            if (!isAnchored(cited, standardAnchors, hhiAnchor)) {
                return ValidationResult.rejected(
                        "HALLUCINATED_NUMBER",
                        "Unanchored numeric value detected: " + rawNumber
                );
            }
        }

        return ValidationResult.accepted();
    }

    private static String maskAllowedTokensForNumberCheck(String text, Set<String> allowedTickers) {
        String result = text.replaceAll("(?i)\\bB3\\b", " ");
        List<String> sortedTickers = allowedTickers.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String ticker : sortedTickers) {
            result = result.replaceAll("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(ticker) + "(?![A-Za-z0-9_])", " ");
        }
        return result;
    }

    private static List<Double> collectStandardAnchors(SanitizedPortfolioSnapshot snapshot) {
        List<Double> anchors = new ArrayList<>();
        for (SanitizedAssetPosition pos : snapshot.positions()) {
            addIfNonNull(anchors, pos.weightPercent());
            if (pos.qualityGrade() != null) {
                anchors.add(pos.qualityGrade().doubleValue());
            }
            addIfNonNull(anchors, pos.priceToEarnings());
            addIfNonNull(anchors, pos.priceToBook());
            addIfNonNull(anchors, pos.dividendYieldPercent());
            addIfNonNull(anchors, pos.grahamMarginPercent());
        }
        for (MacroClassAllocationSnapshot macro : snapshot.macroClassAllocations()) {
            addIfNonNull(anchors, macro.currentPercent());
            addIfNonNull(anchors, macro.targetPercent());
            addIfNonNull(anchors, macro.deviationPercent());
        }
        return anchors;
    }

    private static void addIfNonNull(List<Double> target, BigDecimal value) {
        if (value != null) {
            target.add(value.doubleValue());
        }
    }

    private static boolean isAnchored(double cited, List<Double> standardAnchors, double hhiAnchor) {
        if (Math.abs(cited - hhiAnchor) <= HHI_TOLERANCE) {
            return true;
        }
        for (double anchor : standardAnchors) {
            if (Math.abs(cited - anchor) <= DEFAULT_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> buildExcludedAcronyms() {
        Set<String> set = new HashSet<>(Set.of(
                "HHI", "CVM", "B3", "PL", "PVP", "VP", "DY", "CDI", "IPCA", "SELIC",
                "ETF", "FII", "BDR", "LGPD", "ZDR", "KERNO",
                "ROE", "ROA", "ROIC", "EBITDA", "EBIT", "EV", "CAGR", "WACC", "ESG", "LPA", "VPA",
                "II", "III",
                "CDB", "LCI", "LCA", "CRI", "CRA", "FGC", "IBOV", "IFIX",
                "GPS", "IR", "IOF", "IA", "AI", "LLM", "JSON",
                "BRL", "USD", "EUR", "EUA", "USA", "PT", "BR", "RF", "RV", "PP",
                "DE", "DA", "DO", "DAS", "DOS", "EM", "NA", "NO", "NAS", "NOS",
                "OU", "SE", "UM", "UMA", "OS", "AS", "AO", "AOS", "POR", "COM",
                "SEM", "PARA", "MAIS", "SOB", "ATE", "QUE", "NAO", "SIM"
        ));
        for (MacroClass mc : MacroClass.values()) {
            set.add(mc.name());
        }
        for (CerradoSubClass sc : CerradoSubClass.values()) {
            set.add(sc.name());
        }
        return Set.copyOf(set);
    }

    public record ValidationResult(boolean valid, String reasonCode, String message) {
        public static ValidationResult accepted() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult rejected(String reasonCode, String message) {
            return new ValidationResult(false, reasonCode, message);
        }
    }
}
