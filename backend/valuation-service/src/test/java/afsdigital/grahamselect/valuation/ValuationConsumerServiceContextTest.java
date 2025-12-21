package afsdigital.grahamselect.valuation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.time.Duration;

@SpringBootTest
@EmbeddedKafka(topics = "some-topic", partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
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
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory;

    @Test
    public void dummyTest() {
        kafkaTemplate.send("some-topic","1", "some-message");
        kafkaTemplate.setConsumerFactory(defaultKafkaConsumerFactory);
        ConsumerRecord<String, String> consumerRecord = kafkaTemplate.receive("some-topic", 0, 0, Duration.ofSeconds(1));
        assert consumerRecord != null;
        Assertions.assertThat(consumerRecord.value()).isEqualTo("some-message");
    }

}
