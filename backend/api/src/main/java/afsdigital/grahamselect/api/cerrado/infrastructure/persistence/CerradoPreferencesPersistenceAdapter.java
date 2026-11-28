package afsdigital.grahamselect.api.cerrado.infrastructure.persistence;

import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities.CerradoPreferencesEntity;
import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.repository.CerradoPreferencesJpaRepository;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoPreferencesPort;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CerradoPreferencesPersistenceAdapter implements CerradoPreferencesPort {

    private final CerradoPreferencesJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CerradoPreferences> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(CerradoPreferencesPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public CerradoPreferences save(CerradoPreferences preferences) {
        CerradoPreferencesEntity entity = toEntity(preferences);
        if (entity.getId() == null && entity.getUserId() != null) {
            jpaRepository.findByUserId(entity.getUserId())
                    .ifPresent(existing -> entity.setId(existing.getId()));
        }
        CerradoPreferencesEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    private static CerradoPreferences toDomain(CerradoPreferencesEntity entity) {
        return CerradoPreferences.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .targetAcoesBr(entity.getTargetAcoesBr())
                .targetFiis(entity.getTargetFiis())
                .targetRendaFixa(entity.getTargetRendaFixa())
                .targetInternacional(entity.getTargetInternacional())
                .targetCripto(entity.getTargetCripto())
                .grahamMultiplier(entity.getGrahamMultiplier())
                .grahamFilterEnabled(entity.isGrahamFilterEnabled())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static CerradoPreferencesEntity toEntity(CerradoPreferences domain) {
        return CerradoPreferencesEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .targetAcoesBr(domain.getTargetAcoesBr())
                .targetFiis(domain.getTargetFiis())
                .targetRendaFixa(domain.getTargetRendaFixa())
                .targetInternacional(domain.getTargetInternacional())
                .targetCripto(domain.getTargetCripto())
                .grahamMultiplier(domain.getGrahamMultiplier())
                .grahamFilterEnabled(domain.isGrahamFilterEnabled())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
