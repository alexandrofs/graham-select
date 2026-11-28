package afsdigital.grahamselect.common.ai.domain.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure, versioned allowlist catalog for canonicalizing B3, International,
 * Tesouro Direto, and Crypto tickers. Any ticker or free-text description outside
 * the allowlist (including literal {@code *_MANUAL_*} attempts) resolves to empty
 * so that {@code PiiSanitizer} assigns a deterministic anonymous manual label.
 */
public class TickerAllowlistCatalog {

    public static final String CATALOG_VERSION = "2026.11.1";

    private static final Pattern B3_FRACTIONAL_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]{3}[0-9]{1,2}F$");
    private static final Pattern B3_TICKER_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]{3}[0-9]{1,2}$");
    private static final Pattern INTERNATIONAL_TICKER_PATTERN = Pattern.compile("^[A-Z0-9.-]{1,10}$");
    private static final Pattern TESOURO_PATTERN = Pattern.compile(
            "^TESOURO[ _-]+(SELIC|IPCA\\+?|PREFIXADO|RENDA\\+?|EDUCA\\+?)(?:[ _-]+(\\d{4}))?$"
    );
    private static final Pattern CRYPTO_PAIR_PATTERN = Pattern.compile("^([A-Z0-9]{2,10})-(?:BRL|USD)$");

    private static final int MIN_TESOURO_YEAR = 2020;
    private static final int MAX_TESOURO_YEAR = 2100;

    private static final Set<String> CURATED_B3_TICKERS = Set.of(
            // Acoes B3 principais
            "ABCB4", "ABEV3", "ALOS3", "ALPA4", "ANIM3", "ARZZ3", "ASAI3", "AURE3", "AZUL4",
            "B3SA3", "BBAS3", "BBDC3", "BBDC4", "BBSE3", "BEEF3", "BMGB4", "BPAC11", "BRAP4",
            "BRAV3", "BRFS3", "BRKM5", "BRSR6", "CAML3", "CASH3", "CCRO3", "CMIG4", "COGN3",
            "CPFE3", "CPLE6", "CRFB3", "CSAN3", "CSMG3", "CSNA3", "CURY3", "CXSE3", "CYRE3",
            "DIRR3", "DXCO3", "ECOR3", "EGIE3", "ELET3", "ELET6", "EMBR3", "ENEV3", "EQTL3",
            "EZTC3", "FLRY3", "GGBR4", "GOAU4", "GOLL4", "GRND3", "HAPV3", "HBSA3", "HYPE3",
            "IGTI11", "INTB3", "IRBR3", "ISAE4", "ITSA4", "ITUB3", "ITUB4", "JBSS3", "JHSF3",
            "KLBN4", "KLBN11", "LOGN3", "LREN3", "LWSA3", "MDIA3", "MGLU3", "MLAS3", "MOVI3",
            "MRFG3", "MRVE3", "MULT3", "NEOE3", "NTCO3", "PCAR3", "PETR3", "PETR4", "PETZ3",
            "POMO4", "POSI3", "PRIO3", "PSSA3", "RADL3", "RAIL3", "RAIZ4", "RAPT4", "RDOR3",
            "RECV3", "RENT3", "SANB11", "SAPR4", "SAPR11", "SBSP3", "SIMH3", "SLCE3", "SMTO3",
            "SOMA3", "STBP3", "SUZB3", "TAEE3", "TAEE4", "TAEE11", "TIMS3", "TOTS3", "TUPY3",
            "UGPA3", "USIM5", "VALE3", "VAMO3", "VBBR3", "VIVA3", "VIVT3", "VULC3", "WEGE3",
            "YDUQ3",
            // BDRs
            "AAPL34", "AMZO34", "BABA34", "BERK34", "COCA34", "DISB34", "GOGL34", "INBR32",
            "JNJB34", "JPMC34", "MCDC34", "MELI34", "META34", "MSFT34", "NFLX34", "NUBR33",
            "NVDC34", "PEPB34", "PFIZ34", "ROXO34", "TSLA34", "VISA34", "WALM34", "XPBR31",
            // ETFs Acoes Brasileiras (GetCustodyPositionsUseCase)
            "BOVA11", "BOVV11", "BOVB11", "SMAL11", "SMAC11", "IBOV11",
            "DIVO11", "FIND11", "MATB11", "ISUS11", "ECOO11", "PIBB11",
            "BRAX11", "GOVE11", "TRIG11", "NDIV11", "NSDV11",
            // ETFs Internacionais B3 (GetCustodyPositionsUseCase)
            "IVVB11", "SPXI11", "NASD11", "WRLD11", "ACWI11", "USTK11",
            "EURP11", "XINA11", "USAL11", "TECK11", "SHOT11",
            // ETFs Renda Fixa B3 (GetCustodyPositionsUseCase)
            "IMAB11", "IRFM11", "FIXA11", "B5P211", "LFTS11", "DEBB11", "LFTB11",
            // ETFs Cripto B3 (GetCustodyPositionsUseCase)
            "HASH11", "QBTC11", "BITH11", "ETHE11", "DEFI11", "CRPT11", "WEB311",
            // FIIs e FI-Infras principais
            "ALZR11", "BCFF11", "BDIF11", "BRCO11", "BRCR11", "BTLG11", "BTRA11", "CDII11",
            "CLIN11", "CPTI11", "CPTS11", "DEVA11", "GGRC11", "HCTR11", "HFOF11", "HGBS11",
            "HGRE11", "HGLG11", "HGRU11", "HSML11", "IFRA11", "IRDM11", "JSRE11", "JURO11",
            "KDIF11", "KFOF11", "KNCA11", "KNCR11", "KNHY11", "KNIP11", "KNRI11", "KNSC11",
            "LVBI11", "MALL11", "MCCI11", "MXRF11", "PVBI11", "RBRF11", "RBRP11", "RBRR11",
            "RECR11", "RZAG11", "RZAK11", "RZTR11", "SNAG11", "TEPP11", "TGAR11", "TORD11",
            "TRXF11", "VGIP11", "VGIR11", "VILG11", "VINO11", "VISC11", "VRTA11", "XPCA11",
            "XPLG11", "XPML11"
    );

    private static final Set<String> CURATED_INTERNATIONAL_TICKERS = Set.of(
            "AAPL", "ABNB", "ADBE", "AGG", "AMD", "AMZN", "ARKK", "ASML", "AVGO", "AXP",
            "BAC", "BIL", "BLK", "BND", "BRK.A", "BRK.B", "COST", "CRM", "CRWD", "CVX",
            "DIS", "ETHA", "FBTC", "GLD", "GOOG", "GOOGL", "GS", "HD", "IAU", "IBIT",
            "IEF", "INTC", "IVV", "JEPI", "JEPQ", "JNJ", "JPM", "KO", "LLY", "MA",
            "MCD", "META", "MS", "MSFT", "NET", "NFLX", "NVDA", "NVO", "ORCL", "PANW",
            "PEP", "PG", "PLTR", "PYPL", "QCOM", "QQQ", "SCHD", "SCHW", "SGOV", "SHOP",
            "SHY", "SLV", "SMH", "SNOW", "SOXX", "SPY", "SQ", "TLT", "TSLA", "TSM",
            "UBER", "UNH", "V", "VEA", "VIG", "VNQ", "VOO", "VT", "VTI", "VTV",
            "VUG", "VWO", "VXUS", "VYM", "WFC", "WMT", "XOM"
    );

    private static final Set<String> CURATED_CRYPTO_TICKERS = Set.of(
            "AAVE", "ADA", "ALGO", "APT", "ARB", "ATOM", "AVAX", "BNB", "BTC", "DOGE",
            "DOT", "ETH", "LINK", "LTC", "MATIC", "NEAR", "OP", "POL", "SHIB", "SOL",
            "SUI", "TON", "TRX", "UNI", "USDC", "USDT", "XLM", "XRP"
    );

    private final Set<String> extraAllowedTickers;

    public TickerAllowlistCatalog() {
        this(Set.of());
    }

    public TickerAllowlistCatalog(Set<String> extraAllowedTickers) {
        if (extraAllowedTickers == null || extraAllowedTickers.isEmpty()) {
            this.extraAllowedTickers = Set.of();
        } else {
            Set<String> normalized = new HashSet<>();
            for (String ticker : extraAllowedTickers) {
                if (ticker != null && !ticker.isBlank() && !ticker.toUpperCase(Locale.ROOT).contains("_MANUAL_")) {
                    normalized.add(ticker.trim().toUpperCase(Locale.ROOT));
                }
            }
            this.extraAllowedTickers = Collections.unmodifiableSet(normalized);
        }
    }

    /**
     * Resolves a raw ticker or description into an allowlisted canonical ticker.
     * Returns empty when the input is outside the allowlist or attempts a literal {@code *_MANUAL_*} label.
     */
    public Optional<String> resolveCanonicalTicker(String rawTicker) {
        if (rawTicker == null || rawTicker.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawTicker.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("_MANUAL_")) {
            return Optional.empty();
        }

        Optional<String> tesouro = resolveTesouroDireto(normalized);
        if (tesouro.isPresent()) {
            return tesouro;
        }

        Optional<String> crypto = resolveCrypto(normalized);
        if (crypto.isPresent()) {
            return crypto;
        }

        Optional<String> b3 = resolveB3(normalized);
        if (b3.isPresent()) {
            return b3;
        }

        return resolveInternational(normalized);
    }

    private Optional<String> resolveTesouroDireto(String normalized) {
        Matcher matcher = TESOURO_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String rawType = matcher.group(1);
        String canonicalType = switch (rawType) {
            case "SELIC" -> "SELIC";
            case "IPCA", "IPCA+" -> "IPCA";
            case "PREFIXADO" -> "PREFIXADO";
            case "RENDA", "RENDA+" -> "RENDA_MAIS";
            case "EDUCA", "EDUCA+" -> "EDUCA_MAIS";
            default -> null;
        };
        if (canonicalType == null) {
            return Optional.empty();
        }
        String yearStr = matcher.group(2);
        if (yearStr == null) {
            return Optional.of("TESOURO_" + canonicalType);
        }
        int year = Integer.parseInt(yearStr);
        if (year < MIN_TESOURO_YEAR || year > MAX_TESOURO_YEAR) {
            return Optional.empty();
        }
        return Optional.of("TESOURO_" + canonicalType + "_" + year);
    }

    private Optional<String> resolveCrypto(String normalized) {
        Matcher pairMatcher = CRYPTO_PAIR_PATTERN.matcher(normalized);
        String candidate = pairMatcher.matches() ? pairMatcher.group(1) : normalized;
        if (CURATED_CRYPTO_TICKERS.contains(candidate)) {
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private Optional<String> resolveB3(String normalized) {
        String candidate = normalized;
        if (B3_FRACTIONAL_PATTERN.matcher(normalized).matches()) {
            candidate = normalized.substring(0, normalized.length() - 1);
        }
        if (!B3_TICKER_PATTERN.matcher(candidate).matches()) {
            return Optional.empty();
        }
        if (CURATED_B3_TICKERS.contains(candidate) || extraAllowedTickers.contains(candidate)) {
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private Optional<String> resolveInternational(String normalized) {
        if (!INTERNATIONAL_TICKER_PATTERN.matcher(normalized).matches()) {
            return Optional.empty();
        }
        if (CURATED_INTERNATIONAL_TICKERS.contains(normalized) || extraAllowedTickers.contains(normalized)) {
            return Optional.of(normalized);
        }
        return Optional.empty();
    }
}
