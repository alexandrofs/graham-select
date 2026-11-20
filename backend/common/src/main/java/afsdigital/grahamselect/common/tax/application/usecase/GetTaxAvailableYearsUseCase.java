package afsdigital.grahamselect.common.tax.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.tax.application.dto.TaxAvailableYearsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class GetTaxAvailableYearsUseCase {

    private final TradePort tradePort;

    public TaxAvailableYearsDTO execute(String userId, String profileId) {
        log.info("Buscando anos disponíveis para informe de Bens e Direitos: userId={}, profileId={}", userId, profileId);
        List<Trade> trades = tradePort.findAllByUserId(userId);
        if (trades == null || trades.isEmpty()) {
            return new TaxAvailableYearsDTO(List.of());
        }

        List<Integer> tradeYears = trades.stream()
                .filter(Objects::nonNull)
                .filter(trade -> {
                    if (profileId != null && !profileId.isBlank()) {
                        return profileId.trim().equals(trade.getProfileId());
                    } else {
                        return trade.getProfileId() == null;
                    }
                })
                .map(Trade::getTradeDate)
                .filter(Objects::nonNull)
                .map(java.time.LocalDate::getYear)
                .distinct()
                .sorted()
                .toList();

        if (tradeYears.isEmpty()) {
            return new TaxAvailableYearsDTO(List.of());
        }

        int minYear = tradeYears.get(0);
        int currentYear = java.time.LocalDate.now().getYear();
        int maxYear = Math.max(tradeYears.get(tradeYears.size() - 1), currentYear);

        List<Integer> years = java.util.stream.IntStream.rangeClosed(minYear, maxYear)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();

        return new TaxAvailableYearsDTO(years);
    }

}
