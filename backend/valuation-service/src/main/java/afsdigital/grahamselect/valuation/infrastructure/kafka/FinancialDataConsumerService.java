package afsdigital.grahamselect.valuation.infrastructure.kafka;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.valuation.application.usecase.CalculationIntrinsicValueUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FinancialDataConsumerService {

    private final CalculationIntrinsicValueUseCase calculationIntrinsicValueUseCase;

    @KafkaListener(topics = TopicConstants.FINANCIAL_DATA_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<FinancialDataKey, FinancialDataEvent> record) {

        try {
            FinancialDataKey key = record.key();
            FinancialDataEvent event = record.value();

            log.info("Consumed financial data event for ticker: {} at offset: {}",
                    key != null ? key.getTicker() : "null", record.offset());

            if (event == null) {
                log.warn("Received null event at offset: {}", record.offset());
                return;
            }

            calculationIntrinsicValueUseCase.process(event);

            log.info("Successfully processed financial data event for ticker: {}", event.getTicker());

        } catch (Exception e) {
            log.error("Error processing financial data event at offset: {}. Error: {}",
                    record.offset(), e.getMessage(), e);
        }
    }
}
