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

@SpringBootTest
@EmbeddedKafka(topics = {
        "FINANCIAL_DATA_TOPIC" }, partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class ValuationConsumerServiceContextTest {

    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.28")
            .withDatabaseName("grahamselect")
            .withUsername("root")
            .withPassword("password");

    @DynamicPropertySource
    static void configureTestContainers(DynamicPropertyRegistry registry) {
        mysql.start();
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void contextLoadsSuccessfully() {
        // Verify that the Spring context loads successfully with all beans
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanNamesForType(Object.class).length).isGreaterThan(0);
    }

}
