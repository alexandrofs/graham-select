package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import afsdigital.grahamselect.valuation.application.service.exceptions.InvalidBookValueException;
import afsdigital.grahamselect.valuation.application.service.exceptions.InvalidEarningsPerShareException;

public class IntrinsicValueCalculatorService {

    public static final double GRAHAM_FACTOR = 22.5;

    public Double calculate(FinancialDataDto financialDataDto) {
        validate(financialDataDto);
        if (financialDataDto.eps() < 0 || financialDataDto.bookValuePerShare() < 0) {
            return null;
        }
        return Math.sqrt(GRAHAM_FACTOR * financialDataDto.eps() * financialDataDto.bookValuePerShare());
    }

    private void validate(FinancialDataDto financialDataDto) {
        if (financialDataDto.eps() == null) {
            throw new InvalidEarningsPerShareException("Earnings per share cannot be null");
        }
        if (financialDataDto.bookValuePerShare() == null) {
            throw new InvalidBookValueException("Book value per share cannot be null");
        }
    }

}
