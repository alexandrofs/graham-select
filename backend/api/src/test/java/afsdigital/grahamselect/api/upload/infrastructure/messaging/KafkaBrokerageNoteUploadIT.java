package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatusState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:testdb_kafka_brokerage;DB_CLOSE_DELAY=-1;MODE=MySQL",
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
public class KafkaBrokerageNoteUploadIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private B3ImportStatusPort importStatusPort;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${app.upload.storage-path}")
    private String storagePath;

    private byte[] createPdf(List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(40, 750);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Deve consumir evento de upload de nota PDF, emitir TradeExtractedEvent e concluir processamento")
    void shouldProcessPdfBrokerageNoteAndPublishTradeExtractedEvents() throws Exception {
        String userId = "user-brokerage-123";
        String correlationId = UUID.randomUUID().toString();
        String fileName = "nota_corretagem_test.pdf";
        Path filePath = Path.of(storagePath).toAbsolutePath().resolve(fileName);

        Files.createDirectories(filePath.getParent());

        byte[] pdfBytes = createPdf(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 887766 Data pregão: 22/08/2026 Folha: 1",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D",
                "1-BOVESPA V VISTA VALE3 VALE ON 50 62,10 3.105,00 C"
        ));
        Files.write(filePath, pdfBytes);

        importStatusPort.createPending(correlationId, userId, fileName);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-trade-extracted-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TopicConstants.TRADE_EXTRACTED_TOPIC);

        FileUploadedEvent uploadEvent = FileUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .fileName(fileName)
                .storagePath(filePath.toString())
                .correlationId(correlationId)
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, userId, uploadEvent);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        ConsumerRecord<String, String> record1 = KafkaTestUtils.getSingleRecord(consumer, TopicConstants.TRADE_EXTRACTED_TOPIC, Duration.ofSeconds(15));
        assertThat(record1).isNotNull();
        TradeExtractedEvent event1 = mapper.readValue(record1.value(), TradeExtractedEvent.class);
        assertThat(event1.userId()).isEqualTo(userId);
        assertThat(event1.correlationId()).isEqualTo(correlationId);
        assertThat(event1.ticker()).isEqualTo("PETR4");
        assertThat(event1.side()).isEqualTo("COMPRA");
        assertThat(event1.price()).isEqualByComparingTo("38.50");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId, userId).orElse(null);
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo(B3ImportStatusState.COMPLETED);
        });

        consumer.close();
        Files.deleteIfExists(filePath);
    }
}
