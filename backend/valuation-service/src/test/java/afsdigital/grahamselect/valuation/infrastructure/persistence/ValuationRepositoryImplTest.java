package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValuationRepositoryImplTest {

    @Mock
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @Mock
    private StockPriceJpaRepository stockPriceJpaRepository;

    @InjectMocks
    private ValuationRepositoryImpl valuationRepository;

    private IntrinsicValue intrinsicValue;
    private StockPrice stockPrice;

    @BeforeEach
    void setUp() {
        intrinsicValue = IntrinsicValue.builder()
                .calculationDate(LocalDate.now())
                .companyId("company-123")
                .value(new BigDecimal("100.00"))
                .build();
        stockPrice = StockPrice.builder()
                .date(LocalDate.now())
                .companyId("company-123")
                .price(new BigDecimal("150.00"))
                .build();
    }

    @Test
    void shouldSaveNewIntrinsicValueAndStockPrice() {
        when(intrinsicValueJpaRepository.findByCompanyIdAndCalculationDate(any(), any()))
                .thenReturn(Optional.empty());

        valuationRepository.saveValuationData(intrinsicValue, stockPrice);

        ArgumentCaptor<IntrinsicValueEntity> intrinsicCaptor = ArgumentCaptor.forClass(IntrinsicValueEntity.class);
        verify(intrinsicValueJpaRepository, times(1)).save(intrinsicCaptor.capture());
        assertEquals(new BigDecimal("100.00"), intrinsicCaptor.getValue().getIntrinsicValue());

        verify(stockPriceJpaRepository, times(1)).save(any(StockPriceEntity.class));
    }

    @Test
    void shouldUpdateExistingIntrinsicValueAndSaveStockPrice() {
        IntrinsicValueEntity existingEntity = IntrinsicValueEntity.builder()
                .id("id-123")
                .companyId("company-123")
                .calculationDate(LocalDate.now())
                .intrinsicValue(new BigDecimal("50.00"))
                .build();

        when(intrinsicValueJpaRepository.findByCompanyIdAndCalculationDate(any(), any()))
                .thenReturn(Optional.of(existingEntity));

        valuationRepository.saveValuationData(intrinsicValue, stockPrice);

        ArgumentCaptor<IntrinsicValueEntity> intrinsicCaptor = ArgumentCaptor.forClass(IntrinsicValueEntity.class);
        verify(intrinsicValueJpaRepository, times(1)).save(intrinsicCaptor.capture());
        assertEquals(new BigDecimal("100.00"), intrinsicCaptor.getValue().getIntrinsicValue());

        verify(stockPriceJpaRepository, times(1)).save(any(StockPriceEntity.class));
    }

    @Test
    void shouldSaveStockPriceEvenIfIntrinsicValueThrowsDataIntegrityViolationException() {
        when(intrinsicValueJpaRepository.findByCompanyIdAndCalculationDate(any(), any()))
                .thenReturn(Optional.empty());

        doThrow(new DataIntegrityViolationException("Concurrent insert"))
                .when(intrinsicValueJpaRepository).save(any(IntrinsicValueEntity.class));

        valuationRepository.saveValuationData(intrinsicValue, stockPrice);

        verify(intrinsicValueJpaRepository, times(1)).save(any(IntrinsicValueEntity.class));
        verify(stockPriceJpaRepository, times(1)).save(any(StockPriceEntity.class));
    }
}
