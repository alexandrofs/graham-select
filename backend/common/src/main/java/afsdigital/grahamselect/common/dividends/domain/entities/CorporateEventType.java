package afsdigital.grahamselect.common.dividends.domain.entities;

public enum CorporateEventType {
    DIVIDENDO,
    JCP,
    RENDIMENTO,
    BONIFICACAO,
    DESDOBRAMENTO;

    public static CorporateEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return DIVIDENDO;
        }
        String normalized = value.trim().toUpperCase();
        for (CorporateEventType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        if (normalized.contains("JUR") || normalized.contains("JCP")) {
            return JCP;
        }
        if (normalized.contains("REND")) {
            return RENDIMENTO;
        }
        if (normalized.contains("BONIF")) {
            return BONIFICACAO;
        }
        if (normalized.contains("DESD") || normalized.contains("SPLIT")) {
            return DESDOBRAMENTO;
        }
        return DIVIDENDO;
    }
}
