package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractionFailedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb_dlq;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(topics = {
        TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC
}, partitions = 1)
public class KafkaTradeExtractionDlqIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void shouldPublishToDlqOnExtractionFailure() throws Exception {
        // [P1] Scenario: Validation of DLQ flow for invalid messages
        // Given
        String userId = "user-dlq-456";
        String correlationId = UUID.randomUUID().toString();
        
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("raw", "TICKER;INVALID;DATE");

        TradeExtractionFailedEvent failedEvent = TradeExtractionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId)
                .fileName("invalid.xlsx")
                .lineNumber(10)
                .originalPayload(payloadMap)
                .reason("Unparseable date")
                .build();

        // Setup DLQ Consumer
        Map<String, Object> props = KafkaTestUtils.consumerProps("test-dlq-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        Map<String, Object> consumerProps = new HashMap<>(props);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC);

        // When - Publishing failure event
        kafkaTemplate.send(TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC, correlationId, failedEvent);

        // Then
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC, Duration.ofMillis(10000));
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("Unparseable date");
        assertThat(record.value()).contains("TICKER;INVALID;DATE");

        consumer.close();
    }
}
