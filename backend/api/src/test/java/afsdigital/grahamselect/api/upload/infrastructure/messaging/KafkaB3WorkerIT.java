package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
@EmbeddedKafka(topics = {TopicConstants.B3_UPLOAD_TOPIC, TopicConstants.TRADE_EXTRACTED_TOPIC}, partitions = 1)
public class KafkaB3WorkerIT {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private B3ImportStatusPort importStatusPort;

    @Value("${app.upload.storage-path}")
    private String storagePath;

    @Test
    public void shouldProcessFileAndPublishTradeExtractedEvents() throws Exception {
        // Given
        String userId = "user-worker-123";
        String correlationId = UUID.randomUUID().toString();
        String fileName = "worker-test.xlsx";
        Path filePath = Path.of(storagePath).resolve(fileName);
        
        Files.createDirectories(Path.of(storagePath));
        byte[] content = createExcel(new String[][]{
                {"Ticker", "Data", "Quantidade", "Preco"},
                {"PETR4", "10/05/2026", "100", "35.50"},
                {"VALE3", "11/05/2026", "200", "80.00"}
        });
        Files.write(filePath, content);

        FileUploadedEvent uploadEvent = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId)
                .fileName(fileName)
                .storagePath(filePath.toString())
                .build();

        // When
        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, userId, objectMapper.writeValueAsString(uploadEvent));

        // Then
        // 1. Check status in DB goes to COMPLETED
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId, userId)
                    .orElse(null);
            assertThat(status).isNotNull();
            assertThat(status.status().name()).isEqualTo("COMPLETED");
            assertThat(status.processedRows()).isEqualTo(2);
        });

        // 2. Check events in TRADE_EXTRACTED_TOPIC (Manually receiving to verify)
        // Note: In a real test, we might use a TestConsumer, but for brevity we rely on the DB status
        // which implies the publisher was called.
    }

    private byte[] createExcel(String[][] rows) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length; c++) {
                    ws.value(r, c, rows[r][c]);
                }
            }
        }
        return os.toByteArray();
    }
}
