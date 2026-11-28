package afsdigital.grahamselect.common.cerrado.domain.model;

/**
 * Subclasses analiticas de Nivel 2 do Diagrama do Cerrado.
 */
public enum CerradoSubClass {
    EQUITY_DEFENSIVE(MacroClass.ACOES_BR),
    EQUITY_GROWTH(MacroClass.ACOES_BR),
    FII_BRICK(MacroClass.FIIS),
    FII_PAPER(MacroClass.FIIS),
    FII_FOF(MacroClass.FIIS),
    TREASURY_POST_FIXED(MacroClass.RENDA_FIXA),
    TREASURY_IPCA(MacroClass.RENDA_FIXA),
    TREASURY_PREFIXED(MacroClass.RENDA_FIXA),
    FIXED_INCOME_PRIVATE(MacroClass.RENDA_FIXA),
    INTERNATIONAL_EQUITY(MacroClass.INTERNACIONAL),
    INTERNATIONAL_ETF(MacroClass.INTERNACIONAL),
    CRYPTO_MAJOR(MacroClass.CRIPTO),
    OUTROS_MANUAL(null);

    private final MacroClass macroClass;

    CerradoSubClass(MacroClass macroClass) {
        this.macroClass = macroClass;
    }

    /**
     * Verifica se a subclasse pertence a macro classe informada.
     * {@link #OUTROS_MANUAL} e compativel com qualquer macro classe.
     */
    public boolean belongsTo(MacroClass macroClass) {
        if (macroClass == null) {
            return false;
        }
        return this.macroClass == null || this.macroClass == macroClass;
    }

    /**
     * Indica se a subclasse representa titulo publico soberano (Tesouro Direto),
     * isento do teto individual de risco emissor unico.
     */
    public boolean isSovereignTreasury() {
        return this == TREASURY_POST_FIXED
                || this == TREASURY_IPCA
                || this == TREASURY_PREFIXED;
    }
}
