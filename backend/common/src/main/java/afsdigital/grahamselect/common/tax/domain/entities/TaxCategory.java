package afsdigital.grahamselect.common.tax.domain.entities;

public enum TaxCategory {
    SWING_TRADE_EQUITY,
    FII_FIAGRO,
    ETF,
    BDR;

    public static TaxCategory fromTaxAssetGroup(TaxAssetGroup group) {
        if (group == null) {
            return SWING_TRADE_EQUITY;
        }
        return switch (group) {
            case FII, FIAGRO -> FII_FIAGRO;
            case ETF -> ETF;
            case BDR -> BDR;
            case ACOES, OUTROS -> SWING_TRADE_EQUITY;
        };
    }

    public boolean isCommonBasket() {
        return this != FII_FIAGRO;
    }
}
