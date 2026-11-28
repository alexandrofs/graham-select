package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Lists manual and imported trades owned by a user, ordered most recent first.
 */
@RequiredArgsConstructor
public class ListManualTradesUseCase {

    private final TradePort tradePort;

    public List<Trade> execute(String userId) {
        return tradePort.findAllByUserId(userId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(Trade::getTradeDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Trade::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
