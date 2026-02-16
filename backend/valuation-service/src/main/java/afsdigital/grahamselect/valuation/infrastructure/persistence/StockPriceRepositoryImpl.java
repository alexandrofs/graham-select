package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.StockPriceRepository;
import afsdigital.grahamselect.valuation.domain.entities.StockPrice;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockPriceRepositoryImpl implements StockPriceRepository {

    private final StockPriceJpaRepository stockPriceJpaRepository;

    @Override
    public void save(StockPrice stockPrice) {
        StockPriceEntity entity = StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(stockPrice.getCompanyId())
                .priceDate(stockPrice.getDate())
                .price(stockPrice.getPrice())
                .build();
        stockPriceJpaRepository.save(entity);
    }
}
