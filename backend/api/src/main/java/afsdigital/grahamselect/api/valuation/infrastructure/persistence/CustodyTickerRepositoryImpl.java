package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class CustodyTickerRepositoryImpl implements CustodyTickerPort {

    private final JpaTradeRepository jpaTradeRepository;
    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public List<String> findDistinctActiveTickers() {
        List<String> tradeTickers = jpaTradeRepository.findDistinctTickers();
        List<String> masterTickers = companyJpaRepository.findAllTickers();

        Set<String> distinctTickers = new LinkedHashSet<>();
        addNormalizedTickers(distinctTickers, tradeTickers);
        addNormalizedTickers(distinctTickers, masterTickers);

        return distinctTickers.stream().sorted().toList();
    }

    private void addNormalizedTickers(Set<String> target, List<String> tickers) {
        if (tickers == null) {
            return;
        }
        tickers.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .forEach(target::add);
    }
}

