package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ValuationRepositoryImplIT extends BaseRepositoryIT {

    @Autowired
    private ValuationRepositoryImpl valuationRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    private String companyId;

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();

        CompanyEntity company = CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("AAPL")
                .name("Apple Inc.")
                .build();
        CompanyEntity savedCompany = companyJpaRepository.save(company);
        companyId = savedCompany.getId();
    }

    @Test
    void shouldSaveValuationDataSuccessfully() {
        // Arrange
        LocalDate date = LocalDate.of(2025, 12, 21);
        BigDecimal ivValue = new BigDecimal("150.50");
        BigDecimal priceValue = new BigDecimal("140.00");

        IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                .companyId(companyId)
                .calculationDate(date)
                .value(ivValue)
                .build();

        StockPrice stockPrice = StockPrice.builder()
                .companyId(companyId)
                .date(date)
                .price(priceValue)
                .build();

        // Act
        valuationRepository.saveValuationData(intrinsicValue, stockPrice);

        // Assert
        assertThat(intrinsicValueJpaRepository.findAll())
                .hasSize(1)
                .extracting(IntrinsicValueEntity::getCompanyId)
                .contains(companyId);

        assertThat(stockPriceJpaRepository.findAll())
                .hasSize(1)
                .extracting(StockPriceEntity::getCompanyId)
                .contains(companyId);

        StockPriceEntity savedPrice = stockPriceJpaRepository.findAll().get(0);
        assertThat(savedPrice.getPrice()).isEqualByComparingTo(priceValue);
        assertThat(savedPrice.getPriceDate()).isEqualTo(date);
    }
}
