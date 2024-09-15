package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.valuation.application.dto.FinancialDataDto;
import afsdigital.grahamselect.valuation.application.repository.IntrinsicValueRepository;
import afsdigital.grahamselect.valuation.application.service.CompanyLookupService;
import afsdigital.grahamselect.valuation.application.service.IntrinsicValueCalculatorService;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.InvalidFinancialDataEventException;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CalculationIntrinsicValueUseCaseTest {

    @Mock
    private IntrinsicValueCalculatorService valueCalculatorService;

    @Mock
    private CompanyLookupService companyLookupService;

    @Mock
    private IntrinsicValueRepository intrinsicValueRepository;

    @InjectMocks
    private CalculationIntrinsicValueUseCase calculationIntrinsicValueUseCase;

    @Test
    public void shouldProcessFinancialDataEventSuccessfully() {

        FinancialDataEvent financialDataEvent = FinancialDataEvent.builder().ticker("")
                .ticker("AAPL")
                .earningsPerShare(5.0)
                .bookValuePerShare(20.0)
                .resultDate(LocalDate.of(2024, 1, 1))
                .build();

        when(valueCalculatorService.calculate(any(FinancialDataDto.class))).thenReturn(100.0);

        Company company = new Company("AAPL");

        when(companyLookupService.findOrCreateCompany("AAPL")).thenReturn(company);

        calculationIntrinsicValueUseCase.process(financialDataEvent);

        ArgumentCaptor<IntrinsicValue> intrinsicValueCaptor = ArgumentCaptor.forClass(IntrinsicValue.class);
        verify(intrinsicValueRepository).save(intrinsicValueCaptor.capture());

        IntrinsicValue savedIntrinsicValue = intrinsicValueCaptor.getValue();
        assertNotNull(savedIntrinsicValue);
        assertNotNull(savedIntrinsicValue.getCompanyId());
        assertEquals(LocalDate.of(2024, 1, 1), savedIntrinsicValue.getCalculationDate());
        assertEquals(BigDecimal.valueOf(100.0).round(new MathContext(2)), savedIntrinsicValue.getValue());
    }

    @Test
    public void shouldThrowExceptionWhenFinancialDataEventIsNull() {
        // Act & Assert
        InvalidFinancialDataEventException exception = assertThrows(
                InvalidFinancialDataEventException.class,
                () -> calculationIntrinsicValueUseCase.process(null)
        );

        assertEquals("Financial Data Event and Ticker cannot be null or empty.", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenTickerIsEmpty() {

        FinancialDataEvent financialDataEvent = FinancialDataEvent.builder().ticker("").build();

        InvalidFinancialDataEventException exception = assertThrows(
                InvalidFinancialDataEventException.class,
                () -> calculationIntrinsicValueUseCase.process(financialDataEvent)
        );

        assertEquals("Financial Data Event and Ticker cannot be null or empty.", exception.getMessage());
    }

    @Test
    public void shouldCallServicesWithCorrectParameters() {

        FinancialDataEvent financialDataEvent = FinancialDataEvent.builder().ticker("")
                .ticker("AAPL")
                .earningsPerShare(5.0)
                .bookValuePerShare(20.0)
                .resultDate(LocalDate.of(2024, 1, 1))
                .build();

        when(valueCalculatorService.calculate(any(FinancialDataDto.class))).thenReturn(100.0);

        Company company = new Company("AAPL");
        when(companyLookupService.findOrCreateCompany("AAPL")).thenReturn(company);

        calculationIntrinsicValueUseCase.process(financialDataEvent);

        verify(valueCalculatorService).calculate(new FinancialDataDto(5.0, 20.0));
        verify(companyLookupService).findOrCreateCompany("AAPL");
    }
}
