package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntrinsicValueCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(IntrinsicValueCalculatorService.class);
    public static final double GRAHAM_FACTOR = 22.5;

    public Double calculate(FinancialDataDto financialDataDto) {
        if (financialDataDto.eps() == null || financialDataDto.bookValuePerShare() == null) {
            logger.warn("EPS or Book Value Per Share is null. Ignoring calculation.");
            return null;
        }
        if (financialDataDto.eps() < 0 || financialDataDto.bookValuePerShare() < 0) {
            return null;
        }
        return Math.sqrt(GRAHAM_FACTOR * financialDataDto.eps() * financialDataDto.bookValuePerShare());
    }

}