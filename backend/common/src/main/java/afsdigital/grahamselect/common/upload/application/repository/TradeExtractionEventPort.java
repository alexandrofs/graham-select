package afsdigital.grahamselect.common.upload.application.repository;

import afsdigital.grahamselect.common.upload.domain.events.TradeExtractedEvent;
import afsdigital.grahamselect.common.upload.domain.events.TradeExtractionFailedEvent;

public interface TradeExtractionEventPort {

    void publishExtractedTrade(TradeExtractedEvent event);

    void publishExtractionFailure(TradeExtractionFailedEvent event);
}
