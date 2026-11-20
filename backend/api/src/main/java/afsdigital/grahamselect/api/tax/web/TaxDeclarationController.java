package afsdigital.grahamselect.api.tax.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessGuard;
import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.tax.application.dto.*;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxAssetDeclarationReportUseCase;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxAvailableYearsUseCase;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxMonthlyCapitalGainsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
public class TaxDeclarationController {

    private final GetTaxAssetDeclarationReportUseCase getTaxAssetDeclarationReportUseCase;
    private final GetTaxAvailableYearsUseCase getTaxAvailableYearsUseCase;
    private final GetTaxMonthlyCapitalGainsUseCase getTaxMonthlyCapitalGainsUseCase;
    private final PremiumFeatureAccessGuard premiumFeatureAccessGuard;
    private final FamilyProfilePort familyProfilePort;

    private static final DecimalFormatSymbols PT_BR_SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @GetMapping("/bens-e-direitos")
    public ResponseEntity<TaxAssetDeclarationReportDTO> getBensEDireitos(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "profileId", required = false) String profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous";
        log.info("Recebida requisição GET /api/v1/tax/bens-e-direitos: userId={}, year={}, profileId={}",
                userId, year, profileId);

        try {
            validateYear(year);
            validateFamilyProfileAccess(userId, profileId);
            TaxAssetDeclarationReportDTO report = getTaxAssetDeclarationReportUseCase.execute(userId, year, profileId);
            return ResponseEntity.ok(report);
        } catch (FamilyProfileNotFoundException e) {
            log.error("Perfil familiar não encontrado: userId={}, profileId={}, erro={}", userId, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil familiar não encontrado.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar relatório de Bens e Direitos: userId={}, year={}, profileId={}, erro={}",
                    userId, year, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar relatório de Bens e Direitos", e);
        }
    }

    @GetMapping(value = "/bens-e-direitos/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportBensEDireitosCsv(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "profileId", required = false) String profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous";
        log.info("Recebida requisição GET /api/v1/tax/bens-e-direitos/export: userId={}, year={}, profileId={}",
                userId, year, profileId);

        try {
            validateYear(year);
            validateFamilyProfileAccess(userId, profileId);
            TaxAssetDeclarationReportDTO report = getTaxAssetDeclarationReportUseCase.execute(userId, year, profileId);

            StringBuilder csv = new StringBuilder();
            // UTF-8 BOM
            csv.append('\uFEFF');

            // Header
            csv.append("Grupo;Código;Localização;CNPJ;Ticker;Ativo;Quantidade;Preço Médio;")
                    .append("Situação 31/12/").append(report.previousYear()).append(";")
                    .append("Situação 31/12/").append(report.year()).append(";")
                    .append("Discriminação\n");

            for (TaxAssetDeclarationItemDTO item : report.items()) {
                csv.append(escapeCsvField(item.groupCode())).append(";")
                        .append(escapeCsvField(item.itemCode())).append(";")
                        .append(escapeCsvField(item.locationCode())).append(";")
                        .append(escapeCsvField(item.cnpj())).append(";")
                        .append(escapeCsvField(item.ticker())).append(";")
                        .append(escapeCsvField(item.companyName())).append(";")
                        .append(formatQuantityCsv(item.quantity())).append(";")
                        .append(formatMoneyCsv(item.averagePrice())).append(";")
                        .append(formatMoneyCsv(item.previousYearValue())).append(";")
                        .append(formatMoneyCsv(item.currentYearValue())).append(";")
                        .append(escapeCsvField(item.discrimination())).append("\n");
            }

            byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            String filename = "informe_bens_direitos_" + report.year() + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (FamilyProfileNotFoundException e) {
            log.error("Perfil familiar não encontrado para exportação CSV: userId={}, profileId={}, erro={}", userId, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil familiar não encontrado.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao exportar CSV de Bens e Direitos: userId={}, year={}, profileId={}, erro={}",
                    userId, year, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao exportar CSV de Bens e Direitos", e);
        }
    }

    @GetMapping("/available-years")
    public ResponseEntity<TaxAvailableYearsDTO> getAvailableYears(
            @RequestParam(value = "profileId", required = false) String profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous";
        log.info("Recebida requisição GET /api/v1/tax/available-years: userId={}, profileId={}", userId, profileId);

        try {
            validateFamilyProfileAccess(userId, profileId);
            TaxAvailableYearsDTO years = getTaxAvailableYearsUseCase.execute(userId, profileId);
            return ResponseEntity.ok(years);
        } catch (FamilyProfileNotFoundException e) {
            log.error("Perfil familiar não encontrado para available-years: userId={}, profileId={}, erro={}", userId, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil familiar não encontrado.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar anos disponíveis: userId={}, profileId={}, erro={}",
                    userId, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar anos disponíveis", e);
        }
    }

    @GetMapping("/monthly-capital-gains")
    public ResponseEntity<AnnualTaxSummaryDTO> getMonthlyCapitalGains(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "profileId", required = false) String profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous";
        log.info("Recebida requisição GET /api/v1/tax/monthly-capital-gains: userId={}, year={}, profileId={}",
                userId, year, profileId);

        try {
            validateYear(year);
            validateFamilyProfileAccess(userId, profileId);
            AnnualTaxSummaryDTO summary = getTaxMonthlyCapitalGainsUseCase.execute(userId, year, profileId);
            return ResponseEntity.ok(summary);
        } catch (FamilyProfileNotFoundException e) {
            log.error("Perfil familiar não encontrado para apuração mensal: userId={}, profileId={}, erro={}", userId, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil familiar não encontrado.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar apuração mensal de ganhos de capital: userId={}, year={}, profileId={}, erro={}",
                    userId, year, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar apuração mensal de ganhos de capital", e);
        }
    }

    @GetMapping(value = "/monthly-capital-gains/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportMonthlyCapitalGainsCsv(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "profileId", required = false) String profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : "anonymous";
        log.info("Recebida requisição GET /api/v1/tax/monthly-capital-gains/export: userId={}, year={}, profileId={}",
                userId, year, profileId);

        try {
            validateYear(year);
            validateFamilyProfileAccess(userId, profileId);
            AnnualTaxSummaryDTO summary = getTaxMonthlyCapitalGainsUseCase.execute(userId, year, profileId);

            StringBuilder csv = new StringBuilder();
            // UTF-8 BOM
            csv.append('\uFEFF');

            // Seção 1: Demonstrativo Consolidado dos 12 Meses
            csv.append("Mês;Alienações Ações (R$);Isento Ações;Lucro Isento (R$);Lucro Trib. Comum (R$);Prej. Compensado Comum (R$);Saldo Prej. Comum (R$);Imposto Comum (R$);Lucro Trib. FII (R$);Prej. Compensado FII (R$);Saldo Prej. FII (R$);Imposto FII (R$);Total Imposto Mês (R$);Status DARF;DARF Valor (R$);DARF Vencimento\n");

            for (MonthlyTaxCalculationDTO m : summary.monthlyCalculations()) {
                String monthStr = String.format("%02d/%d", m.month(), m.year());
                String isentoAcoesStr = m.isEquityExempt() ? "Sim" : "Não";
                String vencimentoStr = m.darfDueDate() != null ? m.darfDueDate().format(DATE_FORMATTER) : "-";

                csv.append(escapeCsvField(monthStr)).append(";")
                        .append(formatMoneyCsv(m.totalEquitySales())).append(";")
                        .append(escapeCsvField(isentoAcoesStr)).append(";")
                        .append(formatMoneyCsv(m.exemptProfit())).append(";")
                        .append(formatMoneyCsv(m.commonTaxableProfit())).append(";")
                        .append(formatMoneyCsv(m.commonLossCompensated())).append(";")
                        .append(formatMoneyCsv(m.commonLossCarryforward())).append(";")
                        .append(formatMoneyCsv(m.commonTaxDue())).append(";")
                        .append(formatMoneyCsv(m.fiiTaxableProfit())).append(";")
                        .append(formatMoneyCsv(m.fiiLossCompensated())).append(";")
                        .append(formatMoneyCsv(m.fiiLossCarryforward())).append(";")
                        .append(formatMoneyCsv(m.fiiTaxDue())).append(";")
                        .append(formatMoneyCsv(m.totalTaxDue())).append(";")
                        .append(escapeCsvField(m.darfStatus().name())).append(";")
                        .append(formatMoneyCsv(m.darfAmount())).append(";")
                        .append(escapeCsvField(vencimentoStr)).append("\n");
            }

            // Linha em branco separando as seções
            csv.append("\n");

            // Seção 2: Detalhamento de Alienações do Ano
            csv.append("Data;Ticker;Tipo;Quantidade;Preço Venda (R$);Preço Médio (R$);Valor Alienação (R$);Resultado Líquido (R$);Classificação\n");

            for (MonthlyTaxCalculationDTO m : summary.monthlyCalculations()) {
                for (MonthlyTradeOperationDTO op : m.operations()) {
                    String dataStr = op.tradeDate() != null ? op.tradeDate().format(DATE_FORMATTER) : "-";
                    csv.append(escapeCsvField(dataStr)).append(";")
                            .append(escapeCsvField(op.ticker())).append(";")
                            .append(escapeCsvField(op.side())).append(";")
                            .append(formatQuantityCsv(op.quantity())).append(";")
                            .append(formatMoneyCsv(op.price())).append(";")
                            .append(formatMoneyCsv(op.averagePrice())).append(";")
                            .append(formatMoneyCsv(op.totalSaleValue())).append(";")
                            .append(formatMoneyCsv(op.netProfitOrLoss())).append(";")
                            .append(escapeCsvField(op.category().name())).append("\n");
                }
            }

            byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            String filename = "apuracao_mensal_ganhos_capital_" + summary.year() + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (FamilyProfileNotFoundException e) {
            log.error("Perfil familiar não encontrado para exportação mensal CSV: userId={}, profileId={}, erro={}", userId, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil familiar não encontrado.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao exportar CSV de apuração mensal: userId={}, year={}, profileId={}, erro={}",
                    userId, year, profileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao exportar CSV de apuração mensal", e);
        }
    }

    private void validateYear(Integer year) {
        if (year != null && (year < 2000 || year > 2100)) {
            log.warn("Ano fornecido fora da faixa permitida (2000-2100): {}", year);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano deve estar entre 2000 e 2100.");
        }
    }

    private void validateFamilyProfileAccess(String userId, String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return;
        }

        if (!premiumFeatureAccessGuard.canAccessPremiumFeatures()) {
            log.warn("Tentativa de acesso a perfil familiar fiscal sem plano Premium: userId={}", userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso a perfis familiares requer assinatura Premium.");
        }

        UUID parsedId;
        try {
            parsedId = UUID.fromString(profileId.trim());
        } catch (IllegalArgumentException e) {
            log.error("UUID de profileId inválido: userId={}, profileId={}", userId, profileId, e);
            throw new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
        }

        familyProfilePort.findActiveByIdAndMasterUserId(parsedId, userId)
                .orElseThrow(() -> {
                    log.error("Perfil familiar não ativo ou não pertencente ao usuário: userId={}, profileId={}", userId, parsedId);
                    return new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
                });
    }

    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value;
        if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@")) {
            sanitized = "'" + sanitized;
        }
        if (sanitized.contains(";") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }

    private String formatQuantityCsv(BigDecimal quantity) {
        if (quantity == null) {
            return "0";
        }
        String formatted;
        if (quantity.stripTrailingZeros().scale() <= 0) {
            formatted = quantity.setScale(0, RoundingMode.HALF_UP).toPlainString();
        } else {
            DecimalFormat df = new DecimalFormat("#,##0.####", PT_BR_SYMBOLS);
            formatted = df.format(quantity);
        }
        return escapeCsvField(formatted);
    }

    private String formatMoneyCsv(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        DecimalFormat df = new DecimalFormat("0.00", PT_BR_SYMBOLS);
        String formatted = df.format(value.setScale(2, RoundingMode.HALF_UP));
        return escapeCsvField(formatted);
    }

}
