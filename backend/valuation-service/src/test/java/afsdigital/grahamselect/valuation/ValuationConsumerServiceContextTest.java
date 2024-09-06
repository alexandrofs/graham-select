package afsdigital.grahamselect.valuation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;

@SpringBootTest
@EmbeddedKafka(topics = "some-topic", partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class ValuationConsumerServiceContextTest {

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
