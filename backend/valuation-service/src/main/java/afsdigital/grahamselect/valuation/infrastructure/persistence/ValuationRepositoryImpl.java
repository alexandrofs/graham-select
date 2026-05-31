package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.ValuationRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ValuationRepositoryImpl implements ValuationRepository {

    private final IntrinsicValueJpaRepository intrinsicValueJpaRepository;
    private final StockPriceJpaRepository stockPriceJpaRepository;

    @Override
    @Transactional
    public void saveValuationData(IntrinsicValue intrinsicValue, StockPrice stockPrice) {
        try {
            Optional<IntrinsicValueEntity> existingIntrinsicValue = intrinsicValueJpaRepository
                    .findByCompanyIdAndCalculationDate(intrinsicValue.getCompanyId(), intrinsicValue.getCalculationDate());

            IntrinsicValueEntity intrinsicValueEntity;
            if (existingIntrinsicValue.isPresent()) {
                intrinsicValueEntity = existingIntrinsicValue.get();
                intrinsicValueEntity.setIntrinsicValue(intrinsicValue.getValue());
            } else {
                intrinsicValueEntity = IntrinsicValueEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .companyId(intrinsicValue.getCompanyId())
                        .calculationDate(intrinsicValue.getCalculationDate())
                        .intrinsicValue(intrinsicValue.getValue())
                        .build();
            }
            intrinsicValueJpaRepository.save(intrinsicValueEntity);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent insert detected for company {} on date {}: {}", 
                     intrinsicValue.getCompanyId(), intrinsicValue.getCalculationDate(), e.getMessage());
        }

        StockPriceEntity stockPriceEntity = StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(stockPrice.getCompanyId())
                .priceDate(stockPrice.getDate())
                .price(stockPrice.getPrice())
                .build();
        stockPriceJpaRepository.save(stockPriceEntity);
    }
}
