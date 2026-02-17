package afsdigital.grahamselect.api.upload.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.upload.domain.repository.FinancialEventDataProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaFinancialEventDataProducer implements FinancialEventDataProducer {

    private final KafkaTemplate<FinancialDataKey, FinancialDataEvent> kafkaTemplate;

    @Override
    public void send(FinancialDataEvent financialDataEvent) {

        if (financialDataEvent == null || financialDataEvent.getTicker() == null
                || financialDataEvent.getResultDate() == null) {
            throw new IllegalArgumentException("FinancialDataEvent, ticker, or resultDate must not be null");
        }

        log.info("Sending financial data event for {}", financialDataEvent.getTicker());

        FinancialDataKey key = new FinancialDataKey(financialDataEvent.getTicker(), financialDataEvent.getResultDate());

        kafkaTemplate.send(TopicConstants.FINANCIAL_DATA_TOPIC, key, financialDataEvent)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent message for ticker: {} with offset: {}",
                                financialDataEvent.getTicker(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send message for ticker: {}. Error: {}",
                                financialDataEvent.getTicker(), ex.getMessage(), ex);
                    }
                });
    }

}
