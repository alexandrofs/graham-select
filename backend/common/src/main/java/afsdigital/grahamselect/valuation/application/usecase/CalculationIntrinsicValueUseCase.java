package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.valuation.application.repository.IntrinsicValueRepository;
import afsdigital.grahamselect.valuation.application.service.CompanyLookupService;
import afsdigital.grahamselect.valuation.application.service.IntrinsicValueCalculatorService;
import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.InvalidFinancialDataEventException;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
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
    private final IntrinsicValueRepository intrinsicValueRepository;

    public void process(FinancialDataEvent financialDataEvent) {

        if (financialDataEvent == null || StringUtils.isEmpty(financialDataEvent.getTicker())) {
            throw new InvalidFinancialDataEventException("Financial Data Event and Ticker cannot be null or empty.");
        }

        log.info("Processing {} financial data event", financialDataEvent.getTicker());

        FinancialDataDto financialDataDto = new FinancialDataDto(financialDataEvent.getEarningsPerShare(), financialDataEvent.getBookValuePerShare());

        Double calculatedIntrinsicValue = valueCalculatorService.calculate(financialDataDto);

        Company company = companyLookupService.findOrCreateCompany(financialDataEvent.getTicker());

        IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                .calculationDate(financialDataEvent.getResultDate())
                .companyId(company.getId())
                .value(BigDecimal.valueOf(calculatedIntrinsicValue).round(new MathContext(2)))
                .build();

        intrinsicValueRepository.save(intrinsicValue);

    }

}
