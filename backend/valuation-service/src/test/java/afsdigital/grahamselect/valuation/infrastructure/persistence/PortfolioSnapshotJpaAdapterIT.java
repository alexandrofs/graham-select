package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class PortfolioSnapshotJpaAdapterIT extends BaseRepositoryIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private PortfolioSnapshotJpaAdapter portfolioSnapshotJpaAdapter;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("DELETE FROM trades").executeUpdate();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        entityManager.createNativeQuery("DELETE FROM trades").executeUpdate();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @Test
    void shouldCalculateRealPortfolioSnapshotCorrectly() {
        String userId = "user-999";

        // 1. Create companies
        CompanyEntity petr = CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("PETR4")
                .name("Petrobras")
                .build();
        CompanyEntity vale = CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("VALE3")
                .name("Vale")
                .build();
        companyJpaRepository.save(petr);
        companyJpaRepository.save(vale);

        // 2. Create stock prices
        StockPriceEntity pricePetr = StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(petr.getId())
                .price(new BigDecimal("30.00"))
                .priceDate(LocalDate.now())
                .build();
        StockPriceEntity priceVale = StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(vale.getId())
                .price(new BigDecimal("80.00"))
                .priceDate(LocalDate.now())
                .build();
        stockPriceJpaRepository.save(pricePetr);
        stockPriceJpaRepository.save(priceVale);

        // 3. Insert trades directly in trades table
        entityManager.createNativeQuery("INSERT INTO trades (id, user_id, ticker, trade_date, quantity, price, broker, created_at, side) " +
                "VALUES (:id, :userId, :ticker, :tradeDate, :quantity, :price, :broker, :createdAt, :side)")
                .setParameter("id", UUID.randomUUID().toString())
                .setParameter("userId", userId)
                .setParameter("ticker", "PETR4")
                .setParameter("tradeDate", java.sql.Date.valueOf(LocalDate.now()))
                .setParameter("quantity", new BigDecimal("100"))
                .setParameter("price", new BigDecimal("25.00"))
                .setParameter("broker", "XP")
                .setParameter("createdAt", LocalDateTime.now())
                .setParameter("side", "COMPRA")
                .executeUpdate();

        entityManager.createNativeQuery("INSERT INTO trades (id, user_id, ticker, trade_date, quantity, price, broker, created_at, side) " +
                "VALUES (:id, :userId, :ticker, :tradeDate, :quantity, :price, :broker, :createdAt, :side)")
                .setParameter("id", UUID.randomUUID().toString())
                .setParameter("userId", userId)
                .setParameter("ticker", "VALE3")
                .setParameter("tradeDate", java.sql.Date.valueOf(LocalDate.now()))
                .setParameter("quantity", new BigDecimal("50"))
                .setParameter("price", new BigDecimal("75.00"))
                .setParameter("broker", "XP")
                .setParameter("createdAt", LocalDateTime.now())
                .setParameter("side", "COMPRA")
                .executeUpdate();

        // Let's add a sell trade to test the calculation logic
        entityManager.createNativeQuery("INSERT INTO trades (id, user_id, ticker, trade_date, quantity, price, broker, created_at, side) " +
                "VALUES (:id, :userId, :ticker, :tradeDate, :quantity, :price, :broker, :createdAt, :side)")
                .setParameter("id", UUID.randomUUID().toString())
                .setParameter("userId", userId)
                .setParameter("ticker", "PETR4")
                .setParameter("tradeDate", java.sql.Date.valueOf(LocalDate.now()))
                .setParameter("quantity", new BigDecimal("20"))
                .setParameter("price", new BigDecimal("28.00"))
                .setParameter("broker", "XP")
                .setParameter("createdAt", LocalDateTime.now())
                .setParameter("side", "VENDA")
                .executeUpdate();

        // PETR4 active qty: 100 - 20 = 80
        // VALE3 active qty: 50
        // PETR4 market value: 80 * 30.00 = 2400.00
        // VALE3 market value: 50 * 80.00 = 4000.00
        // Total market value: 6400.00
        // PETR4 pct: (2400 / 6400) * 100 = 37.5%
        // VALE3 pct: (4000 / 6400) * 100 = 62.5%

        // Run
        Map<String, BigDecimal> allocation = portfolioSnapshotJpaAdapter.getCurrentAllocationByUserId(userId);

        assertThat(allocation).hasSize(2);
        assertThat(allocation.get("PETR4")).isEqualByComparingTo("37.5000");
        assertThat(allocation.get("VALE3")).isEqualByComparingTo("62.5000");
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    @EntityScan(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    static class Config {
        @Bean
        public PortfolioSnapshotJpaAdapter portfolioSnapshotJpaAdapter() {
            return new PortfolioSnapshotJpaAdapter();
        }
    }
}
