package afsdigital.grahamselect.common.cerrado.domain.service;

import afsdigital.grahamselect.common.cerrado.domain.model.CerradoTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalogo oficial imutavel de templates de onboarding do Diagrama do Cerrado,
 * sem pre-selecao automatica e com aviso regulatorio CVM explicito.
 */
public class CerradoTemplateCatalog {

    public static final int DEFAULT_NEUTRAL_GRADE = 6;

    public static final String CVM_DISCLAIMER =
            "Os modelos exibidos possuem caráter exclusivamente educacional e ilustrativo, "
                    + "não constituindo recomendação personalizada de investimento, consultoria ou "
                    + "análise de suitability (Resolução CVM nº 19/2021 e nº 20/2021). "
                    + "Ajuste os percentuais livremente conforme seus objetivos e tolerância a risco.";

    private static final List<CerradoTemplate> TEMPLATES = List.of(
            new CerradoTemplate(
                    "EQUILIBRADO_CERRADO",
                    "Modelo Equilibrado Cerrado",
                    "Distribuição balanceada entre geração de renda, crescimento em ações e proteção patrimonial.",
                    new BigDecimal("39.00"),
                    new BigDecimal("30.00"),
                    new BigDecimal("20.00"),
                    new BigDecimal("10.00"),
                    new BigDecimal("1.00")
            ),
            new CerradoTemplate(
                    "MODERADO_PATRIMONIAL",
                    "Modelo Moderado Patrimonial",
                    "Foco em preservação de capital e previsibilidade com maior participação em Renda Fixa e FIIs.",
                    new BigDecimal("20.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("45.00"),
                    new BigDecimal("10.00"),
                    new BigDecimal("0.00")
            ),
            new CerradoTemplate(
                    "ACUMULACAO_ARROJADA",
                    "Modelo Acumulação Arrojada",
                    "Maior exposição a renda variável local, internacional e criptoativos para acumulação de longo prazo.",
                    new BigDecimal("45.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("10.00"),
                    new BigDecimal("15.00"),
                    new BigDecimal("5.00")
            )
    );

    public int getDefaultNeutralGrade() {
        return DEFAULT_NEUTRAL_GRADE;
    }

    public String getCvmDisclaimer() {
        return CVM_DISCLAIMER;
    }

    public List<CerradoTemplate> getTemplates() {
        return TEMPLATES;
    }
}
