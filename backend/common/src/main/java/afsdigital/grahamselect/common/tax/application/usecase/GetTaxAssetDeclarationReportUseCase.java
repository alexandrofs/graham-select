package afsdigital.grahamselect.common.tax.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.tax.application.dto.TaxAssetDeclarationItemDTO;
import afsdigital.grahamselect.common.tax.application.dto.TaxAssetDeclarationReportDTO;
import afsdigital.grahamselect.common.tax.domain.entities.TaxAssetGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class GetTaxAssetDeclarationReportUseCase {

    private final TradePort tradePort;
    private final CompanyRepository companyRepository;

    private static final DecimalFormatSymbols PT_BR_SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "BR"));

    public TaxAssetDeclarationReportDTO execute(String userId, Integer year, String profileId) {
        int reportYear = (year != null) ? year : LocalDate.now(ZoneOffset.UTC).getYear() - 1;
        int previousYear = reportYear - 1;
        LocalDate cutoffPreviousYear = LocalDate.of(previousYear, 12, 31);
        LocalDate cutoffCurrentYear = LocalDate.of(reportYear, 12, 31);

        log.info("Gerando relatório de Bens e Direitos: userId={}, year={}, previousYear={}, profileId={}",
                userId, reportYear, previousYear, profileId);

        List<Trade> trades = tradePort.findAllByUserId(userId);
        if (trades == null) {
            trades = List.of();
        }

        List<Trade> filteredTrades = trades.stream()
                .filter(Objects::nonNull)
                .filter(trade -> {
                    if (profileId != null && !profileId.isBlank()) {
                        return profileId.trim().equals(trade.getProfileId());
                    } else {
                        return trade.getProfileId() == null;
                    }
                })
                .filter(trade -> trade.getTradeDate() != null && !trade.getTradeDate().isAfter(cutoffCurrentYear))
                .filter(trade -> trade.getQuantity() != null && trade.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Trade::getTradeDate)
                        .thenComparing(trade -> "COMPRA".equalsIgnoreCase(trade.getSide()) ? 0 : 1)
                        .thenComparing(trade -> trade.getCreatedAt() != null ? trade.getCreatedAt() : LocalDateTime.MIN))
                .toList();

        Map<String, PositionTracker> trackers = new HashMap<>();

        for (Trade trade : filteredTrades) {
            String rawTicker = trade.getTicker();
            if (rawTicker == null || rawTicker.isBlank()) {
                continue;
            }
            String baseTicker = stripFractionalSuffix(rawTicker);
            PositionTracker tracker = trackers.computeIfAbsent(baseTicker, k -> new PositionTracker(baseTicker));
            tracker.processTrade(trade, cutoffPreviousYear);
        }

        Map<String, Company> companyCache = new HashMap<>();
        List<TaxAssetDeclarationItemDTO> items = new ArrayList<>();

        for (PositionTracker tracker : trackers.values()) {
            BigDecimal qtyPrev = tracker.prevYearState.quantity;
            BigDecimal qtyCurr = tracker.currentYearState.quantity;

            boolean hasPositionCurrent = qtyCurr.compareTo(BigDecimal.ZERO) > 0;
            boolean hadPositionPrev = qtyPrev.compareTo(BigDecimal.ZERO) > 0;

            if (!hasPositionCurrent && !hadPositionPrev) {
                // Ativo zerado em ambas as datas não entra no relatório de Bens e Direitos
                continue;
            }

            boolean liquidated = hadPositionPrev && !hasPositionCurrent;

            String ticker = tracker.ticker;
            Company company = companyCache.computeIfAbsent(ticker, t -> {
                Company c = companyRepository.findByTicker(t);
                if (c == null) {
                    c = companyRepository.findByTicker(ticker);
                }
                return c;
            });

            String companyName = (company != null && company.getName() != null && !company.getName().isBlank())
                    ? company.getName()
                    : ticker;
            String cnpj = (company != null && company.getCnpj() != null && !company.getCnpj().isBlank())
                    ? company.getCnpj().trim()
                    : "CNPJ não informado";

            TaxAssetGroup group = TaxAssetGroup.classify(company, ticker);

            BigDecimal previousYearValue = tracker.prevYearState.totalCost.setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentYearValue = tracker.currentYearState.totalCost.setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentQuantity = liquidated ? BigDecimal.ZERO : qtyCurr;
            BigDecimal averagePrice = liquidated
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : tracker.currentYearState.averagePrice.setScale(2, RoundingMode.HALF_UP);

            String discrimination = buildDiscrimination(
                    ticker,
                    companyName,
                    cnpj,
                    group,
                    tracker.brokers,
                    currentQuantity,
                    qtyPrev,
                    averagePrice,
                    currentYearValue,
                    reportYear,
                    liquidated
            );

            items.add(new TaxAssetDeclarationItemDTO(
                    ticker,
                    companyName,
                    cnpj,
                    group.getGroupCode(),
                    group.getItemCode(),
                    group.getGroupDescription(),
                    "105", // Brasil
                    currentQuantity,
                    averagePrice,
                    previousYearValue,
                    currentYearValue,
                    discrimination,
                    liquidated
            ));
        }

        items.sort(Comparator.comparing(TaxAssetDeclarationItemDTO::groupCode)
                .thenComparing(TaxAssetDeclarationItemDTO::ticker));

        BigDecimal previousYearTotal = items.stream()
                .map(TaxAssetDeclarationItemDTO::previousYearValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentYearTotal = items.stream()
                .map(TaxAssetDeclarationItemDTO::currentYearValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netVariation = currentYearTotal.subtract(previousYearTotal).setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentageVariation = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (previousYearTotal.compareTo(BigDecimal.ZERO) > 0) {
            percentageVariation = netVariation.multiply(BigDecimal.valueOf(100))
                    .divide(previousYearTotal, 2, RoundingMode.HALF_UP);
        }

        Map<String, BigDecimal> subtotalsByGroup = new LinkedHashMap<>();
        for (TaxAssetDeclarationItemDTO item : items) {
            subtotalsByGroup.merge(item.groupCode(), item.currentYearValue(), BigDecimal::add);
        }

        return new TaxAssetDeclarationReportDTO(
                reportYear,
                previousYear,
                previousYearTotal,
                currentYearTotal,
                netVariation,
                percentageVariation,
                subtotalsByGroup,
                items
        );
    }

    private String buildDiscrimination(
            String ticker,
            String companyName,
            String cnpj,
            TaxAssetGroup group,
            Set<String> brokers,
            BigDecimal currentQuantity,
            BigDecimal previousQuantity,
            BigDecimal averagePrice,
            BigDecimal currentTotalCost,
            int year,
            boolean liquidated
    ) {
        String brokerText = buildBrokerText(brokers);
        String assetType = resolveAssetTypeName(group, liquidated ? previousQuantity : currentQuantity);

        if (liquidated) {
            String brokerPart = brokerText.startsWith("Custodiado ")
                    ? "custodiado " + brokerText.substring(11)
                    : brokerText;
            return String.format(
                    "Posição de %s %s de %s (%s), CNPJ: %s, %s, totalmente alienada durante o ano-calendário de %d. Custo total de aquisição em 31/12/%d: R$ 0,00.",
                    formatQuantity(previousQuantity),
                    assetType,
                    companyName,
                    ticker,
                    cnpj,
                    brokerPart,
                    year,
                    year
            );
        }

        return String.format(
                "%s %s de %s (%s), CNPJ: %s. %s. Custo total de aquisição em 31/12/%d: R$ %s (Preço médio de aquisição: R$ %s).",
                formatQuantity(currentQuantity),
                assetType,
                companyName,
                ticker,
                cnpj,
                brokerText,
                year,
                formatMoney(currentTotalCost),
                formatMoney(averagePrice)
        );
    }

    private String buildBrokerText(Set<String> brokers) {
        if (brokers == null || brokers.isEmpty()) {
            return "Custodiado em corretora de valores";
        }
        if (brokers.size() == 1) {
            return "Custodiado na corretora " + brokers.iterator().next();
        }
        return "Custodiado nas corretoras: " + String.join(", ", brokers);
    }

    private String resolveAssetTypeName(TaxAssetGroup group, BigDecimal quantity) {
        boolean singular = quantity != null && quantity.compareTo(BigDecimal.ONE) == 0;
        return switch (group) {
            case ACOES -> singular ? "ação" : "ações";
            case FII -> singular ? "cota do fundo imobiliário" : "cotas do fundo imobiliário";
            case FIAGRO -> singular ? "cota do fundo fiagro" : "cotas do fundo fiagro";
            case BDR -> singular ? "BDR" : "BDRs";
            case ETF -> singular ? "cota do fundo de índice" : "cotas do fundo de índice";
            default -> singular ? "cota" : "cotas";
        };
    }

    private String formatQuantity(BigDecimal qty) {
        if (qty == null) {
            return "0";
        }
        if (qty.stripTrailingZeros().scale() <= 0) {
            return qty.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        DecimalFormat df = new DecimalFormat("#,##0.####", PT_BR_SYMBOLS);
        return df.format(qty);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00", PT_BR_SYMBOLS);
        return df.format(value.setScale(2, RoundingMode.HALF_UP));
    }

    private String stripFractionalSuffix(String ticker) {
        if (ticker == null) {
            return "";
        }
        String normalized = ticker.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith("F") && normalized.length() > 4
                && Character.isDigit(normalized.charAt(normalized.length() - 2))) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static class PositionTracker {
        private final String ticker;
        private final PositionState prevYearState = new PositionState();
        private final PositionState currentYearState = new PositionState();
        private final Set<String> brokers = new TreeSet<>();

        PositionTracker(String ticker) {
            this.ticker = ticker;
        }

        void processTrade(Trade trade, LocalDate cutoffPreviousYear) {
            if (trade.getBroker() != null && !trade.getBroker().isBlank()) {
                brokers.add(trade.getBroker().trim());
            }

            LocalDate tradeDate = trade.getTradeDate();
            boolean isPrevYearOrEarlier = tradeDate != null && !tradeDate.isAfter(cutoffPreviousYear);

            if (isPrevYearOrEarlier) {
                prevYearState.apply(trade.getSide(), trade.getQuantity(), trade.getPrice());
            }
            currentYearState.apply(trade.getSide(), trade.getQuantity(), trade.getPrice());
        }
    }

    private static class PositionState {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal averagePrice = BigDecimal.ZERO;

        void apply(String side, BigDecimal qty, BigDecimal price) {
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            BigDecimal tradePrice = (price != null && price.compareTo(BigDecimal.ZERO) >= 0) ? price : BigDecimal.ZERO;

            if ("COMPRA".equalsIgnoreCase(side)) {
                quantity = quantity.add(qty);
                totalCost = totalCost.add(qty.multiply(tradePrice));
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    averagePrice = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
                }
            } else if ("VENDA".equalsIgnoreCase(side)) {
                quantity = quantity.subtract(qty);
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    quantity = BigDecimal.ZERO;
                    totalCost = BigDecimal.ZERO;
                    averagePrice = BigDecimal.ZERO;
                } else {
                    totalCost = quantity.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
    }

}
