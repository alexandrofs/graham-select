package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveTradeUseCase {
    private final TradePort tradePort;

    public Trade execute(Trade trade) {
        boolean alreadyExists = tradePort.exists(
                trade.getUserId(),
                trade.getTicker(),
                trade.getTradeDate(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getBroker()
        );

        if (alreadyExists) {
            return null;
        }

        return tradePort.save(trade);
    }
}
