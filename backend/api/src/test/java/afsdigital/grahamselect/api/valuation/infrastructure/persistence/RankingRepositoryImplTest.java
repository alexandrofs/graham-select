package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.RankingJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RankingRepositoryImplTest extends BaseRepositoryIT {

    @Autowired
    private RankingJpaRepository rankingJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @Autowired
    private RankingRepository rankingRepository;

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @Test
    void shouldReturnTop20BestRankedCompanies() {
        // Arrange: Create calculations for 25 companies
        IntStream.range(0, 25).forEach(i -> {
            String companyId = "COMPANY_" + i;

            companyJpaRepository.save(CompanyEntity.builder()
                    .id(companyId)
                    .name("Company Name " + i)
                    .build());

            // Latest price
            stockPriceJpaRepository.save(StockPriceEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .companyId(companyId)
                    .priceDate(LocalDate.now())
                    .price(BigDecimal.valueOf(10)) // Price = 10 for all
                    .build());

            // Latest calculation
            // Margin of Safety = (IV / Price) - 1
            // i=0 -> (100 / 10) - 1 = 9
            // i=24 -> (76 / 10) - 1 = 6.6
            intrinsicValueJpaRepository.save(IntrinsicValueEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .companyId(companyId)
                    .calculationDate(LocalDate.now())
                    .intrinsicValue(BigDecimal.valueOf(100 - i))
                    .build());
        });

        // Act
        List<RankedCompany> result = rankingRepository.findTop20BestRanked();

        // Assert
        assertThat(result).hasSize(20);

        // The first should be company with i=0 (IV=100, Price=10, MoS=9)
        assertThat(result.get(0).getSymbol()).isEqualTo("COMPANY_0");
        assertThat(result.get(0).getIntrinsicValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.get(0).getMarginOfSafety().setScale(0)).isEqualByComparingTo(BigDecimal.valueOf(9));

        // The 20th should be company with i=19 (IV=81, Price=10, MoS=7.1)
        assertThat(result.get(19).getSymbol()).isEqualTo("COMPANY_19");

        // Check ordering
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getMarginOfSafety()).isGreaterThanOrEqualTo(result.get(i + 1).getMarginOfSafety());
        }
    }
}
