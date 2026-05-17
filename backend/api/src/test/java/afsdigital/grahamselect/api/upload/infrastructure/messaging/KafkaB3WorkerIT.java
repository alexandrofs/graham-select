package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
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
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb_worker;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "app.upload.storage-path=./test-uploads"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(topics = {
        TopicConstants.B3_UPLOAD_TOPIC,
        TopicConstants.TRADE_EXTRACTED_TOPIC,
        TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC
}, partitions = 1)
public class KafkaB3WorkerIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private B3ImportStatusPort importStatusPort;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${app.upload.storage-path}")
    private String storagePath;

    @Test
    public void shouldProcessFileAndPublishTradeExtractedEvents() throws Exception {
        // Given
        String userId = "user-worker-123";
        String correlationId = UUID.randomUUID().toString();
        String fileName = "worker-test.xlsx";
        Path filePath = Path.of(storagePath).toAbsolutePath().resolve(fileName);
        
        Files.createDirectories(filePath.getParent());
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            ws.value(0, 0, "ticker");
            ws.value(0, 1, "data");
            ws.value(0, 2, "quantidade");
            ws.value(0, 3, "preco");

            ws.value(1, 0, "PETR4");
            ws.value(1, 1, LocalDate.of(2026, 5, 10));
            ws.value(1, 2, new BigDecimal("100"));
            ws.value(1, 3, new BigDecimal("35.50"));

            ws.value(2, 0, "VALE3");
            ws.value(2, 1, LocalDate.of(2026, 5, 11));
            ws.value(2, 2, new BigDecimal("200"));
            ws.value(2, 3, new BigDecimal("80.00"));
        }
        Files.write(filePath, os.toByteArray());

        importStatusPort.createPending(correlationId, userId, fileName);

        FileUploadedEvent uploadEvent = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId)
                .fileName(fileName)
                .storagePath(filePath.toString())
                .build();

        // When - Relying on auto-configured JsonSerializer
        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, userId, uploadEvent);

        // Then
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId, userId)
                    .orElse(null);
            assertThat(status).isNotNull();
            assertThat(status.status().name()).isEqualTo("COMPLETED");
            assertThat(status.processedRows()).isEqualTo(2);
        });
    }

    @Test
    public void shouldPublishToDlqWhenRowIsCorrupted() throws Exception {
        // Given
        String userId = "user-dlq-123";
        String correlationId = UUID.randomUUID().toString();
        String fileName = "dlq-test.xlsx";
        Path filePath = Path.of(storagePath).toAbsolutePath().resolve(fileName);

        Files.createDirectories(filePath.getParent());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            ws.value(0, 0, "ticker");
            ws.value(0, 1, "data");
            ws.value(0, 2, "quantidade");
            ws.value(0, 3, "preco");

            ws.value(1, 0, "PETR4");
            ws.value(1, 1, "invalid-date");
            ws.value(1, 2, "abc");
            ws.value(1, 3, new BigDecimal("35.50"));
        }
        Files.write(filePath, os.toByteArray());

        importStatusPort.createPending(correlationId, userId, fileName);

        FileUploadedEvent uploadEvent = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId)
                .fileName(fileName)
                .storagePath(filePath.toString())
                .build();

        // Setup DLQ Consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-dlq-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC);

        // When
        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, userId, uploadEvent);

        // Then
        // 1. Verify message in DLQ
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC, Duration.ofMillis(20000));
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("PETR4");
        assertThat(record.value()).contains("Wrong cell type");

        // 2. Verify status in DB
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId, userId)
                    .orElse(null);
            assertThat(status).isNotNull();
            assertThat(status.status().name()).isEqualTo("COMPLETED");
            assertThat(status.failedRows()).isEqualTo(1);
        });

        consumer.close();
    }
}
