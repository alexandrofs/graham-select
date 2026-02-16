package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.IntrinsicValueRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IntrinsicValueRepositoryImpl implements IntrinsicValueRepository {

    private final IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @Override
    public void save(IntrinsicValue intrinsicValue) {
        IntrinsicValueEntity intrinsicValueEntity = IntrinsicValueEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(intrinsicValue.getCompanyId())
                .calculationDate(intrinsicValue.getCalculationDate())
                .intrinsicValue(intrinsicValue.getValue())
                .build();
        intrinsicValueJpaRepository.save(intrinsicValueEntity);
    }

    @Override
    public List<IntrinsicValue> findTop20BestRanked() {
        return intrinsicValueJpaRepository.findTop20BestRanked().stream()
                .map(entity -> IntrinsicValue.builder()
                        .companyId(entity.getCompanyId())
                        .calculationDate(entity.getCalculationDate())
                        .value(entity.getIntrinsicValue())
                        .build())
                .toList();
    }

}
