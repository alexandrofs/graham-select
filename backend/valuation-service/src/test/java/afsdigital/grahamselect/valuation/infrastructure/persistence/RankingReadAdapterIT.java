package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class RankingReadAdapterIT extends BaseRepositoryIT {

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private RankingReadAdapter rankingReadAdapter;

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @Test
    void shouldFetchTopRankedCompanies() {
        // Create 3 companies
        CompanyEntity c1 = companyJpaRepository.save(CompanyEntity.builder().id(UUID.randomUUID().toString()).ticker("PETR4").name("Petrobras").build());
        CompanyEntity c2 = companyJpaRepository.save(CompanyEntity.builder().id(UUID.randomUUID().toString()).ticker("VALE3").name("Vale").build());
        CompanyEntity c3 = companyJpaRepository.save(CompanyEntity.builder().id(UUID.randomUUID().toString()).ticker("WEGE3").name("Weg").build());

        // Save Intrinsic Values
        intrinsicValueJpaRepository.save(IntrinsicValueEntity.builder().id(UUID.randomUUID().toString()).companyId(c1.getId()).intrinsicValue(new BigDecimal("40.00")).calculationDate(LocalDate.now()).build());
        intrinsicValueJpaRepository.save(IntrinsicValueEntity.builder().id(UUID.randomUUID().toString()).companyId(c2.getId()).intrinsicValue(new BigDecimal("80.00")).calculationDate(LocalDate.now()).build());
        intrinsicValueJpaRepository.save(IntrinsicValueEntity.builder().id(UUID.randomUUID().toString()).companyId(c3.getId()).intrinsicValue(new BigDecimal("20.00")).calculationDate(LocalDate.now()).build());

        // Save Stock Prices
        stockPriceJpaRepository.save(StockPriceEntity.builder().id(UUID.randomUUID().toString()).companyId(c1.getId()).price(new BigDecimal("30.00")).priceDate(LocalDate.now()).build());
        stockPriceJpaRepository.save(StockPriceEntity.builder().id(UUID.randomUUID().toString()).companyId(c2.getId()).price(new BigDecimal("70.00")).priceDate(LocalDate.now()).build());
        stockPriceJpaRepository.save(StockPriceEntity.builder().id(UUID.randomUUID().toString()).companyId(c3.getId()).price(new BigDecimal("25.00")).priceDate(LocalDate.now()).build());

        // margin of safety calculation: (iv / price) - 1
        // PETR4: (40 / 30) - 1 = 0.3333 (first)
        // VALE3: (80 / 70) - 1 = 0.1428 (second)
        // WEGE3: (20 / 25) - 1 = -0.2000 (third)

        List<RankedCompany> list = rankingReadAdapter.findTop20BestRanked();

        assertThat(list).hasSize(3);
        assertThat(list.get(0).getSymbol()).isEqualTo("PETR4");
        assertThat(list.get(0).getMarginOfSafety()).isEqualByComparingTo("0.3333");

        assertThat(list.get(1).getSymbol()).isEqualTo("VALE3");
        assertThat(list.get(1).getMarginOfSafety()).isEqualByComparingTo("0.1429"); // native query division on MySQL might return 0.142857... which matches 0.1429 when rounded/compared

        assertThat(list.get(2).getSymbol()).isEqualTo("WEGE3");
        assertThat(list.get(2).getMarginOfSafety()).isEqualByComparingTo("-0.2000");
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    @EntityScan(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    static class Config {
        @Bean
        public RankingReadAdapter rankingReadAdapter() {
            return new RankingReadAdapter();
        }
    }
}
