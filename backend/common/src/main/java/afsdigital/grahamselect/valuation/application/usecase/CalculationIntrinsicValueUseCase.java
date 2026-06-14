package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.valuation.application.repository.ValuationRepository;
import afsdigital.grahamselect.valuation.application.service.CompanyLookupService;
import afsdigital.grahamselect.valuation.application.service.IntrinsicValueCalculatorService;
import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.InvalidFinancialDataEventException;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;

@Slf4j
@RequiredArgsConstructor
public class CalculationIntrinsicValueUseCase {

    private final IntrinsicValueCalculatorService valueCalculatorService;
    private final CompanyLookupService companyLookupService;
    private final ValuationRepository valuationRepository;

    public void process(FinancialDataEvent financialDataEvent) {
        validate(financialDataEvent);

        log.info("Processing {} financial data event", financialDataEvent.getTicker());

        Double calculatedIntrinsicValue = valueCalculatorService.calculate(
                new FinancialDataDto(
                        financialDataEvent.getEarningsPerShare(),
                        financialDataEvent.getBookValuePerShare()));

        Company company = companyLookupService.findOrCreateCompany(financialDataEvent.getTicker());

        BigDecimal finalIntrinsicValue = calculatedIntrinsicValue != null
                ? BigDecimal.valueOf(calculatedIntrinsicValue).round(new MathContext(2))
                : null;

        BigDecimal epsDecimal = financialDataEvent.getEarningsPerShare() != null
                ? BigDecimal.valueOf(financialDataEvent.getEarningsPerShare())
                : null;
        BigDecimal bvpsDecimal = financialDataEvent.getBookValuePerShare() != null
                ? BigDecimal.valueOf(financialDataEvent.getBookValuePerShare())
                : null;

        IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                .calculationDate(financialDataEvent.getResultDate())
                .companyId(company.getId())
                .value(finalIntrinsicValue)
                .eps(epsDecimal)
                .bvps(bvpsDecimal)
                .build();

        StockPrice stockPrice = StockPrice.builder()
                .companyId(company.getId())
                .date(financialDataEvent.getResultDate())
                .price(BigDecimal.valueOf(financialDataEvent.getPrice()))
                .build();

        valuationRepository.saveValuationData(intrinsicValue, stockPrice);
    }

    private void validate(FinancialDataEvent financialDataEvent) {
        if (financialDataEvent == null || StringUtils.isEmpty(financialDataEvent.getTicker())) {
            throw new InvalidFinancialDataEventException("Financial Data Event and Ticker cannot be null or empty.");
        }
    }
}
