package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
public class CustodyTickerRepositoryImpl implements CustodyTickerPort {

    private final JpaTradeRepository jpaTradeRepository;

    @Override
    public List<String> findDistinctActiveTickers() {
        return jpaTradeRepository.findDistinctTickers();
    }
}
