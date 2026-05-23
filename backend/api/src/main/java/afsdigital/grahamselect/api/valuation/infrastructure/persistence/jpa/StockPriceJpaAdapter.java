package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.StockPricePort;
import afsdigital.grahamselect.valuation.domain.entities.StockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockPriceJpaAdapter implements StockPricePort {

    private final StockPriceJpaRepository repository;

    @Override
    public Optional<StockPrice> findLatestByCompanyId(String companyId) {
        return repository.findFirstByCompanyIdOrderByPriceDateDesc(companyId)
                .map(this::mapToDomain);
    }

    @Override
    public Map<String, StockPrice> findLatestByCompanyIds(List<String> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Map.of();
        }
        return repository.findLatestByCompanyIds(companyIds).stream()
                .collect(Collectors.toMap(
                        StockPriceEntity::getCompanyId,
                        this::mapToDomain,
                        (existing, replacement) -> existing // In case of duplicates on the same date
                ));
    }

    private StockPrice mapToDomain(StockPriceEntity entity) {
        return StockPrice.builder()
                .companyId(entity.getCompanyId())
                .price(entity.getPrice())
                .date(entity.getPriceDate())
                .build();
    }
}
