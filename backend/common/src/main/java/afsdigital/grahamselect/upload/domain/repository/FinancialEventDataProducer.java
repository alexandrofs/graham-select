package afsdigital.grahamselect.upload.domain.repository;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;

public interface FinancialEventDataProducer {

    void send(FinancialDataEvent financialDataEvent);

}
