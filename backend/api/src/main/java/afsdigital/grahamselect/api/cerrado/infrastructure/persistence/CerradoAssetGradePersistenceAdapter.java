package afsdigital.grahamselect.api.cerrado.infrastructure.persistence;

import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities.CerradoAssetGradeEntity;
import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.repository.CerradoAssetGradeJpaRepository;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoAssetGradePort;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetGrade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CerradoAssetGradePersistenceAdapter implements CerradoAssetGradePort {

    private final CerradoAssetGradeJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CerradoAssetGrade> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(CerradoAssetGradePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CerradoAssetGrade> findByUserIdAndTicker(Long userId, String ticker) {
        return jpaRepository.findByUserIdAndTicker(userId, ticker)
                .map(CerradoAssetGradePersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public CerradoAssetGrade save(CerradoAssetGrade assetGrade) {
        CerradoAssetGradeEntity entity = toEntity(assetGrade);
        CerradoAssetGradeEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    private static CerradoAssetGrade toDomain(CerradoAssetGradeEntity entity) {
        return CerradoAssetGrade.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ticker(entity.getTicker())
                .macroClass(entity.getMacroClass())
                .subClass(entity.getSubClass())
                .qualityGrade(entity.getQualityGrade())
                .customRiskCeiling(entity.getCustomRiskCeiling())
                .quarantineCorporateEvent(entity.isQuarantineCorporateEvent())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static CerradoAssetGradeEntity toEntity(CerradoAssetGrade domain) {
        return CerradoAssetGradeEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .ticker(domain.getTicker())
                .macroClass(domain.getMacroClass())
                .subClass(domain.getSubClass())
                .qualityGrade(domain.getQualityGrade())
                .customRiskCeiling(domain.getCustomRiskCeiling())
                .quarantineCorporateEvent(domain.isQuarantineCorporateEvent())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
