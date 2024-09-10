package afsdigital.grahamselect.upload.application.service;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.upload.domain.repository.FinancialEventDataProducer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FinancialDataPublisherService {

    private final FinancialEventDataProducer eventDataProducer;

    public void publish(FinancialDataEvent event) {
        eventDataProducer.send(event);
    }

}
