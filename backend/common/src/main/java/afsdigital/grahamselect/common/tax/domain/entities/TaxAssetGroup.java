package afsdigital.grahamselect.common.tax.domain.entities;

import afsdigital.grahamselect.common.domain.entities.Company;
import lombok.Getter;

import java.util.Locale;
import java.util.Set;

@Getter
public enum TaxAssetGroup {

    ACOES("03", "01", "Participações societárias", "ações"),
    FII("07", "03", "Fundos", "cotas do fundo imobiliário"),
    FIAGRO("07", "04", "Fundos", "cotas do fundo fiagro"),
    BDR("04", "04", "Aplicações e Investimentos", "BDRs"),
    ETF("07", "09", "Fundos", "cotas do fundo de índice"),
    OUTROS("99", "99", "Outros Bens e Direitos", "cotas");

    private static final Set<String> KNOWN_ETFS = Set.of(
            "BOVA11", "SMAL11", "IVVB11", "HASH11", "DIVO11", "FIXA11",
            "GOLD11", "BRAX11", "ECOO11", "MATB11", "NDIV11", "NSDV11",
            "PIBB11", "SPXI11", "TECK11", "ACWI11", "XINA11", "EURP11",
            "NASD11", "BBSD11", "ISUS11", "GENB11"
    );

    private static final Set<String> KNOWN_UNITS = Set.of(
            "TAEE11", "SANB11", "KLBN11", "ALUP11", "SAPR11", "BPAC11",
            "CPLE11", "ENGI11", "TIET11", "SULA11", "RANI11", "MODL11",
            "PPLA11"
    );

    private final String groupCode;
    private final String itemCode;
    private final String groupDescription;
    private final String assetTypeName;

    TaxAssetGroup(String groupCode, String itemCode, String groupDescription, String assetTypeName) {
        this.groupCode = groupCode;
        this.itemCode = itemCode;
        this.groupDescription = groupDescription;
        this.assetTypeName = assetTypeName;
    }

    public static TaxAssetGroup classify(Company company, String normalizedTicker) {
        if (company != null && company.getAssetClass() != null && !company.getAssetClass().isBlank()) {
            String assetClass = company.getAssetClass().toUpperCase(Locale.ROOT);
            if (assetClass.contains("FII") || assetClass.contains("IMOBILIAR")) {
                return FII;
            }
            if (assetClass.contains("FIAGRO")) {
                return FIAGRO;
            }
            if (assetClass.contains("BDR")) {
                return BDR;
            }
            if (assetClass.contains("ETF") || assetClass.contains("INDICE") || assetClass.contains("ÍNDICE")) {
                return ETF;
            }
            if (assetClass.contains("ACAO") || assetClass.contains("AÇÃO") || assetClass.contains("ACOES")
                    || assetClass.contains("AÇÕES") || assetClass.contains("STOCK")) {
                return ACOES;
            }
        }

        if (normalizedTicker == null || normalizedTicker.isBlank()) {
            return OUTROS;
        }

        String ticker = normalizedTicker.trim().toUpperCase(Locale.ROOT);

        if (ticker.endsWith("31") || ticker.endsWith("32") || ticker.endsWith("33")
                || ticker.endsWith("34") || ticker.endsWith("35")) {
            return BDR;
        }

        if (ticker.endsWith("11")) {
            if (KNOWN_ETFS.contains(ticker)) {
                return ETF;
            }
            if (KNOWN_UNITS.contains(ticker)) {
                return ACOES;
            }
            return FII;
        }

        if (ticker.endsWith("3") || ticker.endsWith("4") || ticker.endsWith("5")
                || ticker.endsWith("6") || ticker.endsWith("1") || ticker.endsWith("2")) {
            return ACOES;
        }

        return ACOES;
    }

}
