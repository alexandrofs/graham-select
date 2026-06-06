package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;

public interface MarketDataEventPublisherPort {
    void publish(FinancialDataEvent event);
}
