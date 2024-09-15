package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import afsdigital.grahamselect.valuation.application.service.exceptions.InvalidBookValueException;
import afsdigital.grahamselect.valuation.application.service.exceptions.InvalidEarningsPerShareException;

public class IntrinsicValueCalculatorService {

    public static final double GRAHAM_FACTOR = 22.5;

    public Double calculate(FinancialDataDto financialDataDto) {
        validate(financialDataDto);
        return  Math.sqrt(GRAHAM_FACTOR * financialDataDto.eps() * financialDataDto.bookValuePerShare());
    }

    private static void validate(FinancialDataDto financialDataDto) {
        if (financialDataDto.eps() == null || financialDataDto.eps() <= 0) {
            throw new InvalidEarningsPerShareException("Earnings per share cannot be null or less than 0");
        }
        if (financialDataDto.bookValuePerShare() == null || financialDataDto.bookValuePerShare() <= 0) {
            throw new InvalidBookValueException("Book value per share cannot be null or less than 0");
        }
    }

}
