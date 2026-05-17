package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb_dedup;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "app.upload.storage-path=./test-uploads-dedup"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(topics = {
        TopicConstants.B3_UPLOAD_TOPIC,
        TopicConstants.TRADE_EXTRACTED_TOPIC
}, partitions = 1)
public class TradeDeduplicationIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private B3ImportStatusPort importStatusPort;

    @Autowired
    private JpaTradeRepository tradeRepository;

    @Value("${app.upload.storage-path}")
    private String storagePath;

    @Test
    public void shouldNotImportDuplicateTrades() throws Exception {
        // Given
        String userId = "user-dedup-123";
        String correlationId1 = UUID.randomUUID().toString();
        String fileName = "dedup-test.xlsx";
        Path filePath = Path.of(storagePath).toAbsolutePath().resolve(fileName);
        
        Files.createDirectories(filePath.getParent());
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            ws.value(0, 0, "ticker");
            ws.value(0, 1, "data");
            ws.value(0, 3, "quantidade");
            ws.value(0, 4, "preco");
            ws.value(0, 6, "corretora");

            ws.value(1, 0, "PETR4");
            ws.value(1, 1, LocalDate.of(2026, 5, 10));
            ws.value(1, 3, new BigDecimal("100"));
            ws.value(1, 4, new BigDecimal("35.50"));
            ws.value(1, 6, "Corretora A");
        }
        Files.write(filePath, os.toByteArray());

        // First upload
        importStatusPort.createPending(correlationId1, userId, fileName);
        FileUploadedEvent event1 = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId1)
                .fileName(fileName)
                .storagePath(filePath.toString())
                .build();

        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, userId, event1);

        // Wait for first processing
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId1, userId).orElseThrow();
            assertThat(status.status().name()).startsWith("COMPLETED");
            assertThat(tradeRepository.count()).isEqualTo(1);
        });

        // Second upload of the same file
        String correlationId2 = UUID.randomUUID().toString();
        importStatusPort.createPending(correlationId2, userId, fileName);
        FileUploadedEvent event2 = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId2)
                .fileName(fileName)
                .storagePath(filePath.toString())
                .build();

        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, userId, event2);

        // Then
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId2, userId).orElseThrow();
            assertThat(status.status().name()).startsWith("COMPLETED");
            assertThat(status.duplicatedRows()).isEqualTo(1);
            assertThat(tradeRepository.count()).isEqualTo(1); // Still 1
            assertThat(status.message()).contains("1 duplicada");
        });
    }
}
