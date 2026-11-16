package afsdigital.grahamselect.common.dividends.application.usecase;

import afsdigital.grahamselect.common.dividends.application.dto.RelevantFactDTO;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactPort;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GetRelevantFactsUseCase {

    private final RelevantFactPort relevantFactPort;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;

    public List<RelevantFactDTO> execute(
            String userId,
            String specificTicker,
            Instant startDate,
            Instant endDate,
            int limit,
            boolean onlyMyTickers
    ) {
        log.info("Consultando fatos relevantes: userId={}, ticker={}, limit={}, onlyMyTickers={}",
                userId, specificTicker, limit, onlyMyTickers);

        Set<String> myTickers = Collections.emptySet();
        if (userId != null && !userId.isBlank() && getCustodyPositionsUseCase != null) {
            try {
                List<CustodyPositionDTO> positions = getCustodyPositionsUseCase.execute(userId);
                if (positions != null) {
                    myTickers = positions.stream()
                            .filter(p -> p != null && p.ticker() != null && !p.ticker().isBlank()
                                    && p.quantity() != null && p.quantity().compareTo(BigDecimal.ZERO) > 0)
                            .map(p -> p.ticker().trim().toUpperCase())
                            .collect(Collectors.toSet());
                }
            } catch (Exception e) {
                log.error("Erro ao obter posições para fatos relevantes de userId={}: {}", userId, e.getMessage(), e);
            }
        }

        List<String> tickersFilter = null;
        if (onlyMyTickers) {
            if (myTickers.isEmpty()) {
                return Collections.emptyList();
            }
            if (specificTicker != null && !specificTicker.isBlank()) {
                String clean = specificTicker.trim().toUpperCase();
                if (!myTickers.contains(clean)) {
                    return Collections.emptyList();
                }
                tickersFilter = List.of(clean);
            } else {
                tickersFilter = myTickers.stream().toList();
            }
        } else if (specificTicker != null && !specificTicker.isBlank()) {
            tickersFilter = List.of(specificTicker.trim().toUpperCase());
        }

        List<RelevantFact> facts = relevantFactPort.findByTickersAndDateRange(
                tickersFilter,
                startDate,
                endDate,
                limit
        );

        return facts.stream()
                .map(fact -> new RelevantFactDTO(
                        fact.getId(),
                        fact.getTicker(),
                        fact.getTitle(),
                        fact.getSummary(),
                        fact.getSourceUrl(),
                        fact.getCategory(),
                        fact.getPublicationDate()
                ))
                .toList();
    }
}
