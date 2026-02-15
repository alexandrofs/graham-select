package afsdigital.grahamselect.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@DirtiesContext
@EmbeddedKafka(topics = {
        TopicConstants.FINANCIAL_DATA_TOPIC }, partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class FinancialDataConsumerServiceIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.28")
            .withDatabaseName("grahamselect")
            .withUsername("root")
            .withPassword("password");

    @DynamicPropertySource
    static void configureTestContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private KafkaTemplate<FinancialDataKey, FinancialDataEvent> kafkaTemplate;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @Test
    void shouldConsumeEventAndSaveIntrinsicValueToDatabase() {
        // Given
        FinancialDataKey key = new FinancialDataKey("VALE3", LocalDate.of(2024, 12, 31));
        FinancialDataEvent event = FinancialDataEvent.builder()
                .ticker("VALE3")
                .resultDate(LocalDate.of(2024, 12, 31))
                .price(65.50)
                .earningsPerShare(5.50)
                .bookValuePerShare(30.00)
                .dividendYield(6.5)
                .priceEarnings(11.9)
                .build();

        // When
        kafkaTemplate.send(TopicConstants.FINANCIAL_DATA_TOPIC, key, event);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CompanyEntity> companies = companyJpaRepository.findAll();
            assertThat(companies).hasSize(1);
            assertThat(companies.get(0).getTicker()).isEqualTo("VALE3");

            List<IntrinsicValueEntity> intrinsicValues = intrinsicValueJpaRepository.findAll();
            assertThat(intrinsicValues).hasSize(1);

            IntrinsicValueEntity savedValue = intrinsicValues.get(0);
            assertThat(savedValue.getCalculationDate()).isEqualTo(LocalDate.of(2024, 12, 31));
            assertThat(savedValue.getIntrinsicValue()).isNotNull();
            assertThat(savedValue.getIntrinsicValue()).isGreaterThan(BigDecimal.ZERO);
        });
    }

    @Test
    void shouldHandleMultipleEventsForSameCompany() {
        // Given
        LocalDate date1 = LocalDate.of(2024, 9, 30);
        LocalDate date2 = LocalDate.of(2024, 12, 31);

        FinancialDataKey key1 = new FinancialDataKey("PETR4", date1);
        FinancialDataEvent event1 = FinancialDataEvent.builder()
                .ticker("PETR4")
                .resultDate(date1)
                .price(38.50)
                .earningsPerShare(3.20)
                .bookValuePerShare(18.00)
                .build();

        FinancialDataKey key2 = new FinancialDataKey("PETR4", date2);
        FinancialDataEvent event2 = FinancialDataEvent.builder()
                .ticker("PETR4")
                .resultDate(date2)
                .price(40.00)
                .earningsPerShare(3.50)
                .bookValuePerShare(19.00)
                .build();

        // When
        kafkaTemplate.send(TopicConstants.FINANCIAL_DATA_TOPIC, key1, event1);
        kafkaTemplate.send(TopicConstants.FINANCIAL_DATA_TOPIC, key2, event2);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CompanyEntity> companies = companyJpaRepository.findAll();
            assertThat(companies).hasSize(1);
            assertThat(companies.get(0).getTicker()).isEqualTo("PETR4");

            List<IntrinsicValueEntity> intrinsicValues = intrinsicValueJpaRepository.findAll();
            assertThat(intrinsicValues).hasSize(2);
        });
    }
}
