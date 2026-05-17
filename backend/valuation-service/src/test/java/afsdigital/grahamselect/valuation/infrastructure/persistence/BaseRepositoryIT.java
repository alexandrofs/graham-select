package afsdigital.grahamselect.valuation.infrastructure.persistence;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import afsdigital.grahamselect.common.upload.application.repository.B3FileStoragePort;
import afsdigital.grahamselect.common.upload.application.repository.B3UploadEventPort;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using TestContainers with MySQL.
 * This class provides a shared MySQL container configuration for all repository
 * tests.
 */
@Testcontainers
@SpringBootTest
@EmbeddedKafka(topics = {
        TopicConstants.FINANCIAL_DATA_TOPIC }, partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public abstract class BaseRepositoryIT {

    @MockBean
    private B3FileStoragePort b3FileStoragePort;

    @MockBean
    private B3UploadEventPort b3UploadEventPort;

    @MockBean
    private B3ImportStatusPort b3ImportStatusPort;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.28")
            .withDatabaseName("grahamselect_test")
            .withUsername("root")
            .withPassword("password");

    @DynamicPropertySource
    static void configureTestContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
    }

}
