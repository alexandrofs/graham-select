package afsdigital.grahamselect.valuation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.mock.mockito.MockBean;
import afsdigital.grahamselect.common.upload.application.repository.B3FileStoragePort;
import afsdigital.grahamselect.common.upload.application.repository.B3UploadEventPort;

@SpringBootTest
@EmbeddedKafka(topics = {
        "FINANCIAL_DATA_TOPIC" }, partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class ValuationConsumerServiceContextTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private B3FileStoragePort b3FileStoragePort;

    @MockBean
    private B3UploadEventPort b3UploadEventPort;

    @Test
    public void contextLoadsSuccessfully() {
        // Verify that the Spring context loads successfully with all beans
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanNamesForType(Object.class).length).isGreaterThan(0);
    }

}
