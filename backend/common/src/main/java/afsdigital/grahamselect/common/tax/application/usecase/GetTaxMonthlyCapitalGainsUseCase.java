package afsdigital.grahamselect.common.tax.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.tax.application.dto.AnnualTaxSummaryDTO;
import afsdigital.grahamselect.common.tax.application.dto.MonthlyTaxCalculationDTO;
import afsdigital.grahamselect.common.tax.application.dto.MonthlyTradeOperationDTO;
import afsdigital.grahamselect.common.tax.domain.entities.*;
import afsdigital.grahamselect.common.tax.domain.utils.DarfDueDateCalculator;
import afsdigital.grahamselect.common.tax.domain.utils.TaxTickerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class GetTaxMonthlyCapitalGainsUseCase {

    private final TradePort tradePort;
    private final CompanyRepository companyRepository;

    private static final BigDecimal TWENTY_THOUSAND = new BigDecimal("20000.00");
    private static final BigDecimal TEN = new BigDecimal("10.00");
    private static final BigDecimal COMMON_TAX_RATE = new BigDecimal("0.15");
    private static final BigDecimal FII_TAX_RATE = new BigDecimal("0.20");

    public AnnualTaxSummaryDTO execute(String userId, Integer year, String profileId) {
        int reportYear = (year != null) ? year : LocalDate.now(ZoneOffset.UTC).getYear();
        LocalDate cutoffCurrentYear = LocalDate.of(reportYear, 12, 31);

        log.info("Iniciando apuração mensal de ganhos de capital: userId={}, year={}, profileId={}",
                userId, reportYear, profileId);

        List<Trade> allTrades = tradePort.findAllByUserId(userId);
        if (allTrades == null) {
            allTrades = List.of();
        }

        List<Trade> filteredTrades = allTrades.stream()
                .filter(Objects::nonNull)
                .filter(trade -> {
                    if (profileId != null && !profileId.isBlank()) {
                        return profileId.trim().equals(trade.getProfileId());
                    } else {
                        return trade.getProfileId() == null || trade.getProfileId().isBlank();
                    }
                })
                .filter(trade -> trade.getTradeDate() != null && !trade.getTradeDate().isAfter(cutoffCurrentYear))
                .filter(trade -> trade.getQuantity() != null && trade.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Trade::getTradeDate)
                        .thenComparing(trade -> "COMPRA".equalsIgnoreCase(trade.getSide()) ? 0 : 1)
                        .thenComparing(trade -> trade.getCreatedAt() != null ? trade.getCreatedAt() : LocalDateTime.MIN)
                        .thenComparing(trade -> trade.getId() != null ? trade.getId().toString() : ""))
                .toList();

        Map<String, AssetPosition> positions = new HashMap<>();
        Map<YearMonth, List<MonthlyTradeOperation>> monthlySales = new HashMap<>();
        Map<String, Company> companyCache = new HashMap<>();

        int earliestYear = reportYear;
        for (Trade trade : filteredTrades) {
            String rawTicker = trade.getTicker();
            if (rawTicker == null || rawTicker.isBlank()) {
                continue;
            }
            int tradeYear = trade.getTradeDate().getYear();
            if (tradeYear < earliestYear) {
                earliestYear = tradeYear;
            }

            String baseTicker = TaxTickerUtils.stripFractionalSuffix(rawTicker);
            AssetPosition position = positions.computeIfAbsent(baseTicker, AssetPosition::new);

            if ("COMPRA".equalsIgnoreCase(trade.getSide())) {
                position.buy(trade.getQuantity(), trade.getPrice());
            } else if ("VENDA".equalsIgnoreCase(trade.getSide())) {
                Company company = companyCache.computeIfAbsent(baseTicker, companyRepository::findByTicker);
                TaxAssetGroup group = TaxAssetGroup.classify(company, baseTicker);
                TaxCategory category = TaxCategory.fromTaxAssetGroup(group);

                MonthlyTradeOperation op = position.sell(trade, category, baseTicker);
                YearMonth ym = YearMonth.from(trade.getTradeDate());
                monthlySales.computeIfAbsent(ym, k -> new ArrayList<>()).add(op);
            }
        }

        BigDecimal accumulatedLossCommon = BigDecimal.ZERO;
        BigDecimal accumulatedLossFii = BigDecimal.ZERO;
        BigDecimal accumulatedTaxUnder10 = BigDecimal.ZERO;

        List<MonthlyTaxCalculationDTO> reportYearMonths = new ArrayList<>();

        for (int y = earliestYear; y <= reportYear; y++) {
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(y, m);
                List<MonthlyTradeOperation> ops = monthlySales.getOrDefault(ym, List.of());

                BigDecimal totalEquitySales = BigDecimal.ZERO;
                BigDecimal equityNetResult = BigDecimal.ZERO;
                BigDecimal etfNetResult = BigDecimal.ZERO;
                BigDecimal bdrNetResult = BigDecimal.ZERO;
                BigDecimal fiiNetResult = BigDecimal.ZERO;

                for (MonthlyTradeOperation op : ops) {
                    BigDecimal saleVal = op.totalSaleValue();
                    BigDecimal netRes = op.netProfitOrLoss();
                    switch (op.category()) {
                        case SWING_TRADE_EQUITY -> {
                            totalEquitySales = totalEquitySales.add(saleVal);
                            equityNetResult = equityNetResult.add(netRes);
                        }
                        case ETF -> etfNetResult = etfNetResult.add(netRes);
                        case BDR -> bdrNetResult = bdrNetResult.add(netRes);
                        case FII_FIAGRO -> fiiNetResult = fiiNetResult.add(netRes);
                    }
                }

                boolean isEquityExempt = totalEquitySales.compareTo(TWENTY_THOUSAND) <= 0;
                BigDecimal exemptProfit = BigDecimal.ZERO;
                BigDecimal commonNetResult;

                if (isEquityExempt) {
                    if (equityNetResult.compareTo(BigDecimal.ZERO) > 0) {
                        exemptProfit = equityNetResult;
                        commonNetResult = etfNetResult.add(bdrNetResult);
                    } else {
                        commonNetResult = etfNetResult.add(bdrNetResult).add(equityNetResult);
                    }
                } else {
                    commonNetResult = equityNetResult.add(etfNetResult).add(bdrNetResult);
                }

                // Cesta Comum
                BigDecimal commonTaxableProfit = BigDecimal.ZERO;
                BigDecimal commonLossCompensated = BigDecimal.ZERO;
                BigDecimal commonTaxDue = BigDecimal.ZERO;

                if (commonNetResult.compareTo(BigDecimal.ZERO) > 0) {
                    commonTaxableProfit = commonNetResult;
                    BigDecimal compensated = commonNetResult.min(accumulatedLossCommon);
                    BigDecimal taxableBase = commonNetResult.subtract(compensated);
                    accumulatedLossCommon = accumulatedLossCommon.subtract(compensated);
                    commonLossCompensated = compensated;
                    commonTaxDue = taxableBase.multiply(COMMON_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
                } else if (commonNetResult.compareTo(BigDecimal.ZERO) < 0) {
                    accumulatedLossCommon = accumulatedLossCommon.add(commonNetResult.abs());
                }

                // Cesta FII
                BigDecimal fiiTaxableProfit = BigDecimal.ZERO;
                BigDecimal fiiLossCompensated = BigDecimal.ZERO;
                BigDecimal fiiTaxDue = BigDecimal.ZERO;

                if (fiiNetResult.compareTo(BigDecimal.ZERO) > 0) {
                    fiiTaxableProfit = fiiNetResult;
                    BigDecimal compensatedFii = fiiNetResult.min(accumulatedLossFii);
                    BigDecimal taxableBaseFii = fiiNetResult.subtract(compensatedFii);
                    accumulatedLossFii = accumulatedLossFii.subtract(compensatedFii);
                    fiiLossCompensated = compensatedFii;
                    fiiTaxDue = taxableBaseFii.multiply(FII_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
                } else if (fiiNetResult.compareTo(BigDecimal.ZERO) < 0) {
                    accumulatedLossFii = accumulatedLossFii.add(fiiNetResult.abs());
                }

                BigDecimal totalTaxDueMonth = commonTaxDue.add(fiiTaxDue).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalARecolher = totalTaxDueMonth.add(accumulatedTaxUnder10).setScale(2, RoundingMode.HALF_UP);

                DarfStatus darfStatus;
                BigDecimal darfAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                LocalDate darfDueDate = null;

                if (totalARecolher.compareTo(BigDecimal.ZERO) > 0 && totalARecolher.compareTo(TEN) < 0) {
                    accumulatedTaxUnder10 = totalARecolher;
                    if (ops.isEmpty() && totalTaxDueMonth.compareTo(BigDecimal.ZERO) == 0) {
                        darfStatus = DarfStatus.NO_TAX_DUE;
                    } else {
                        darfStatus = DarfStatus.ACCUMULATED_BELOW_MINIMUM;
                    }
                } else if (totalARecolher.compareTo(TEN) >= 0) {
                    darfStatus = DarfStatus.DUE;
                    darfAmount = totalARecolher;
                    darfDueDate = DarfDueDateCalculator.calculateDueDate(ym);
                    accumulatedTaxUnder10 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                } else {
                    accumulatedTaxUnder10 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    if (commonNetResult.compareTo(BigDecimal.ZERO) < 0 || fiiNetResult.compareTo(BigDecimal.ZERO) < 0) {
                        darfStatus = DarfStatus.LOSS;
                    } else if (exemptProfit.compareTo(BigDecimal.ZERO) > 0) {
                        darfStatus = DarfStatus.EXEMPT;
                    } else {
                        darfStatus = DarfStatus.NO_TAX_DUE;
                    }
                }

                if (y == reportYear) {
                    List<MonthlyTradeOperationDTO> opDTOs = ops.stream()
                            .map(o -> new MonthlyTradeOperationDTO(
                                    o.ticker(),
                                    o.tradeDate(),
                                    o.side(),
                                    o.quantity().setScale(4, RoundingMode.HALF_UP),
                                    o.price().setScale(2, RoundingMode.HALF_UP),
                                    o.averagePrice().setScale(4, RoundingMode.HALF_UP),
                                    o.totalSaleValue().setScale(2, RoundingMode.HALF_UP),
                                    o.netProfitOrLoss().setScale(2, RoundingMode.HALF_UP),
                                    o.category()
                            ))
                            .toList();

                    MonthlyTaxCalculationDTO monthCalc = new MonthlyTaxCalculationDTO(
                            m,
                            y,
                            totalEquitySales.setScale(2, RoundingMode.HALF_UP),
                            isEquityExempt,
                            exemptProfit.setScale(2, RoundingMode.HALF_UP),
                            commonTaxableProfit.setScale(2, RoundingMode.HALF_UP),
                            commonLossCompensated.setScale(2, RoundingMode.HALF_UP),
                            accumulatedLossCommon.setScale(2, RoundingMode.HALF_UP),
                            commonTaxDue.setScale(2, RoundingMode.HALF_UP),
                            fiiTaxableProfit.setScale(2, RoundingMode.HALF_UP),
                            fiiLossCompensated.setScale(2, RoundingMode.HALF_UP),
                            accumulatedLossFii.setScale(2, RoundingMode.HALF_UP),
                            fiiTaxDue.setScale(2, RoundingMode.HALF_UP),
                            totalTaxDueMonth,
                            accumulatedTaxUnder10.setScale(2, RoundingMode.HALF_UP),
                            darfStatus,
                            darfAmount,
                            darfDueDate,
                            opDTOs
                    );
                    reportYearMonths.add(monthCalc);
                }
            }
        }

        BigDecimal totalTaxDueYear = BigDecimal.ZERO;
        BigDecimal totalSalesYear = BigDecimal.ZERO;
        BigDecimal totalExemptProfitYear = BigDecimal.ZERO;
        BigDecimal totalTaxableProfitYear = BigDecimal.ZERO;

        for (MonthlyTaxCalculationDTO m : reportYearMonths) {
            totalTaxDueYear = totalTaxDueYear.add(m.totalTaxDue());
            totalExemptProfitYear = totalExemptProfitYear.add(m.exemptProfit());
            totalTaxableProfitYear = totalTaxableProfitYear.add(m.commonTaxableProfit()).add(m.fiiTaxableProfit());
            for (MonthlyTradeOperationDTO op : m.operations()) {
                totalSalesYear = totalSalesYear.add(op.totalSaleValue());
            }
        }

        return new AnnualTaxSummaryDTO(
                reportYear,
                reportYearMonths,
                totalTaxDueYear.setScale(2, RoundingMode.HALF_UP),
                totalSalesYear.setScale(2, RoundingMode.HALF_UP),
                totalExemptProfitYear.setScale(2, RoundingMode.HALF_UP),
                totalTaxableProfitYear.setScale(2, RoundingMode.HALF_UP),
                accumulatedLossCommon.setScale(2, RoundingMode.HALF_UP),
                accumulatedLossFii.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private static class AssetPosition {
        private final String ticker;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal averagePrice = BigDecimal.ZERO;

        public AssetPosition(String ticker) {
            this.ticker = ticker;
        }

        public void buy(BigDecimal qty, BigDecimal price) {
            if (qty == null || price == null || qty.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) < 0) {
                return;
            }
            quantity = quantity.add(qty);
            totalCost = totalCost.add(qty.multiply(price));
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                averagePrice = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
            }
        }

        public MonthlyTradeOperation sell(Trade trade, TaxCategory category, String baseTicker) {
            BigDecimal qty = trade.getQuantity();
            BigDecimal salePrice = trade.getPrice() != null ? trade.getPrice() : BigDecimal.ZERO;
            BigDecimal saleValue = qty.multiply(salePrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgPriceUsed = averagePrice;

            BigDecimal cost;
            if (qty.compareTo(quantity) > 0) {
                log.warn("Venda superior à custódia para {}: vendida={}, custodia={}", baseTicker, qty, quantity);
                cost = totalCost;
                quantity = BigDecimal.ZERO;
                totalCost = BigDecimal.ZERO;
                averagePrice = BigDecimal.ZERO;
            } else {
                cost = qty.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
                quantity = quantity.subtract(qty);
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    quantity = BigDecimal.ZERO;
                    totalCost = BigDecimal.ZERO;
                    averagePrice = BigDecimal.ZERO;
                } else {
                    totalCost = quantity.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
                }
            }

            BigDecimal result = saleValue.subtract(cost).setScale(2, RoundingMode.HALF_UP);

            return new MonthlyTradeOperation(
                    baseTicker,
                    trade.getTradeDate(),
                    trade.getSide(),
                    qty,
                    salePrice,
                    avgPriceUsed,
                    saleValue,
                    result,
                    category
            );
        }
    }
}
