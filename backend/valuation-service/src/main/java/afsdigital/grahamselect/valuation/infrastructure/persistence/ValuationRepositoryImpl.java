package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.ValuationRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.valuation.domain.entities.StockPrice;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class ValuationRepositoryImpl implements ValuationRepository {

    private final IntrinsicValueJpaRepository intrinsicValueJpaRepository;
    private final StockPriceJpaRepository stockPriceJpaRepository;

    @Override
    @Transactional
    public void saveValuationData(IntrinsicValue intrinsicValue, StockPrice stockPrice) {
        IntrinsicValueEntity intrinsicValueEntity = IntrinsicValueEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(intrinsicValue.getCompanyId())
                .calculationDate(intrinsicValue.getCalculationDate())
                .intrinsicValue(intrinsicValue.getValue())
                .build();
        intrinsicValueJpaRepository.save(intrinsicValueEntity);

        StockPriceEntity stockPriceEntity = StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(stockPrice.getCompanyId())
                .priceDate(stockPrice.getDate())
                .price(stockPrice.getPrice())
                .build();
        stockPriceJpaRepository.save(stockPriceEntity);
    }
}
