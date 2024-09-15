package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import afsdigital.grahamselect.valuation.application.service.exceptions.InvalidBookValueException;
import afsdigital.grahamselect.valuation.application.service.exceptions.InvalidEarningsPerShareException;
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
    public void shouldThrowInvalidEarningsPerShareExceptionWhenEpsIsNull() {

        FinancialDataDto financialDataDto = new FinancialDataDto(null, 20.0);

        InvalidEarningsPerShareException exception = assertThrows(
                InvalidEarningsPerShareException.class,
                () -> intrinsicValueCalculatorService.calculate(financialDataDto)
        );
        assertEquals("Earnings per share cannot be null or less than 0", exception.getMessage());
    }

    @Test
    public void shouldThrowInvalidEarningsPerShareExceptionWhenEpsIsLessThanZero() {

        FinancialDataDto financialDataDto = new FinancialDataDto(-1.0, 20.0);

        InvalidEarningsPerShareException exception = assertThrows(
                InvalidEarningsPerShareException.class,
                () -> intrinsicValueCalculatorService.calculate(financialDataDto)
        );
        assertEquals("Earnings per share cannot be null or less than 0", exception.getMessage());
    }

    @Test
    public void shouldThrowInvalidBookValueExceptionWhenBookValueIsNull() {

        FinancialDataDto financialDataDto = new FinancialDataDto(5.0, null);

        InvalidBookValueException exception = assertThrows(
                InvalidBookValueException.class,
                () -> intrinsicValueCalculatorService.calculate(financialDataDto)
        );
        assertEquals("Book value per share cannot be null or less than 0", exception.getMessage());
    }

    @Test
    public void shouldThrowInvalidBookValueExceptionWhenBookValueIsLessThanZero() {

        FinancialDataDto financialDataDto = new FinancialDataDto(5.0, -1.0);

        InvalidBookValueException exception = assertThrows(
                InvalidBookValueException.class,
                () -> intrinsicValueCalculatorService.calculate(financialDataDto)
        );
        assertEquals("Book value per share cannot be null or less than 0", exception.getMessage());
    }

}
