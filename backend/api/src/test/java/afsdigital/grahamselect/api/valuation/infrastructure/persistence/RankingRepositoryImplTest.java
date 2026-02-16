package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.RankingJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    private RankingRepository rankingRepository;

    @BeforeEach
    void setUp() {
        rankingJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
    }

    @Test
    void shouldReturnTop20BestRankedCompanies() {
        // Arrange: Create calculations for 25 companies
        IntStream.range(0, 25).forEach(i -> {
            String companyId = UUID.randomUUID().toString();

            // Older calculation (should be ignored)
            IntrinsicValueEntity oldIv = IntrinsicValueEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .companyId(companyId)
                    .calculationDate(LocalDate.now().minusDays(10))
                    .intrinsicValue(BigDecimal.valueOf(1000)) // High IV but old
                    .build();
            rankingJpaRepository.save(oldIv);

            // Latest price
            stockPriceJpaRepository.save(StockPriceEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .companyId(companyId)
                    .priceDate(LocalDate.now())
                    .price(BigDecimal.valueOf(10)) // Price = 10 for all
                    .build());

            // Latest calculation
            // Ratio = (100 - i) / 10
            // i=0 -> 100/10=10 (best)
            // i=24 -> 76/10=7.6 (worst)
            IntrinsicValueEntity latestIv = IntrinsicValueEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .companyId(companyId)
                    .calculationDate(LocalDate.now())
                    .intrinsicValue(BigDecimal.valueOf(100 - i))
                    .build();
            rankingJpaRepository.save(latestIv);
        });

        // Act
        List<IntrinsicValue> result = rankingRepository.findTop20BestRanked();

        // Assert
        assertThat(result).hasSize(20);

        // The first should be company with i=0 (IV=100, Ratio=10)
        assertThat(result.get(0).getValue()).isEqualByComparingTo(BigDecimal.valueOf(100));

        // The 20th should be company with i=19 (IV=81, Ratio=8.1)
        assertThat(result.get(19).getValue()).isEqualByComparingTo(BigDecimal.valueOf(81));

        // Check ordering
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getValue()).isGreaterThanOrEqualTo(result.get(i + 1).getValue());
        }
    }
}
