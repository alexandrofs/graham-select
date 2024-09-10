package afsdigital.grahamselect.common.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
public class FinancialDataEvent {

    private LocalDate resultDate; //Data do resultado financeiro
    private String ticker; // TICKER
    private Double price; // PRECO
    private Double dividendYield; // DY
    private Double priceEarnings; // P/L
    private Double priceToBook; // P/VP
    private Double priceToAssets; // P/ATIVOS
    private Double grossMargin; // MARGEM BRUTA
    private Double ebitMargin; // MARGEM EBIT
    private Double netMargin; // MARG. LIQUIDA
    private Double priceToEbit; // P/EBIT
    private Double evToEbit; // EV/EBIT
    private Double netDebtToEbit; // DIVIDA LIQUIDA / EBIT
    private Double netDebtToEquity; // DIV. LIQ. / PATRI.
    private Double priceToSales; // PSR
    private Double priceToWorkingCapital; // P/CAP. GIRO
    private Double priceToCurrentAssets; // P. AT CIR. LIQ.
    private Double currentLiquidity; // LIQ. CORRENTE
    private Double roe; // ROE
    private Double roa; // ROA
    private Double roic; // ROIC
    private Double equityToAssets; // PATRIMONIO / ATIVOS
    private Double liabilitiesToAssets; // PASSIVOS / ATIVOS
    private Double assetTurnover; // GIRO ATIVOS
    private Double revenueCAGR5Years; // CAGR RECEITAS 5 ANOS
    private Double profitCAGR5Years; // CAGR LUCROS 5 ANOS
    private Double averageDailyLiquidity; // LIQUIDEZ MEDIA DIARIA
    private Double bookValuePerShare; // VPA
    private Double earningsPerShare; // LPA
    private Double pegRatio; // PEG Ratio
    private Double marketValue; // VALOR DE MERCADO

}
