package afsdigital.grahamselect.common.portfolio.application.repository;

import afsdigital.grahamselect.common.portfolio.domain.events.ValuationRequestedEvent;

public interface ValuationEventPort {
    void publishValuationRequest(ValuationRequestedEvent event);
}
