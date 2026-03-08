package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntrinsicValueCalculatorServiceTest {

    private IntrinsicValueCalculatorService intrinsicValueCalculatorService;

    @BeforeEach
    public void setUp() {
        intrinsicValueCalculatorService = new IntrinsicValueCalculatorService();
    }

    @Test
    public void shouldCalculateIntrinsicValueSuccessfully() {

        FinancialDataDto financialDataDto = new FinancialDataDto(5.0, 20.0);

        Double intrinsicValue = intrinsicValueCalculatorService.calculate(financialDataDto);

        assertNotNull(intrinsicValue);
        assertEquals(Math.sqrt(22.5 * 5.0 * 20.0), intrinsicValue);
    }

    @Test
    public void shouldReturnNullWhenEpsIsNull() {

        FinancialDataDto financialDataDto = new FinancialDataDto(null, 20.0);

        Double intrinsicValue = intrinsicValueCalculatorService.calculate(financialDataDto);
        assertNull(intrinsicValue);
    }

    @Test
    public void shouldReturnNullWhenEpsIsLessThanZero() {

        FinancialDataDto financialDataDto = new FinancialDataDto(-1.0, 20.0);

        Double intrinsicValue = intrinsicValueCalculatorService.calculate(financialDataDto);
        assertNull(intrinsicValue);
    }

    @Test
    public void shouldReturnNullWhenBookValueIsNull() {

        FinancialDataDto financialDataDto = new FinancialDataDto(5.0, null);

        Double intrinsicValue = intrinsicValueCalculatorService.calculate(financialDataDto);
        assertNull(intrinsicValue);
    }

    @Test
    public void shouldReturnNullWhenBookValueIsLessThanZero() {

        FinancialDataDto financialDataDto = new FinancialDataDto(5.0, -1.0);

        Double intrinsicValue = intrinsicValueCalculatorService.calculate(financialDataDto);
        assertNull(intrinsicValue);
    }

}
