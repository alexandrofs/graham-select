package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

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
    "spring.datasource.url=jdbc:h2:mem:testdb_consumer;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(topics = {
        TopicConstants.TRADE_EXTRACTED_TOPIC
}, partitions = 1)
public class KafkaTradeExtractedConsumerIT {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private B3ImportStatusPort importStatusPort;

    @Autowired
    private TradePort tradePort;

    @Autowired
    private JpaTradeRepository tradeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldHandleRaceConditionAndDeduplicateTrades() throws Exception {
        // [P0] Scenario: Multiple messages for the same trade (Race Condition)
        // Given
        String userId = "user-race-123";
        String correlationId = UUID.randomUUID().toString();
        String ticker = "PETR4";
        LocalDate tradeDate = LocalDate.of(2026, 5, 15);
        BigDecimal quantity = new BigDecimal("100");
        BigDecimal price = new BigDecimal("35.00");

        importStatusPort.createPending(correlationId, userId, "race-test.xlsx");
        importStatusPort.markProcessing(correlationId, userId);

        TradeExtractedEvent event = TradeExtractedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .version("v1")
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .userId(userId)
                .correlationId(correlationId)
                .ticker(ticker)
                .tradeDate(tradeDate)
                .quantity(quantity)
                .price(price)
                .broker("XP")
                .build();

        String payload = objectMapper.writeValueAsString(event);

        // When - Sending two identical messages rapidly
        kafkaTemplate.send(TopicConstants.TRADE_EXTRACTED_TOPIC, userId + ":" + ticker, payload);
        kafkaTemplate.send(TopicConstants.TRADE_EXTRACTED_TOPIC, userId + ":" + ticker, payload);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            B3ImportStatus status = importStatusPort.findByCorrelationIdAndUserId(correlationId, userId)
                    .orElse(null);
            assertThat(status).isNotNull();
            
            // Should have 1 success and 1 duplicate recorded
            // Note: KafkaTradeExtractedConsumer logs it and calls importStatusPort.addDuplicate
            assertThat(status.duplicatedRows()).isEqualTo(1);
        });

        // Verify only one trade exists in DB
        // Use the actual repository to count for verification
        long count = tradeRepository.count();
        assertThat(count).isEqualTo(1);
    }
}
