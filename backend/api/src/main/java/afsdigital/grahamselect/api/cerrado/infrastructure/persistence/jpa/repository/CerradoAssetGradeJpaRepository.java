package afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities.CerradoAssetGradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CerradoAssetGradeJpaRepository extends JpaRepository<CerradoAssetGradeEntity, Long> {

    List<CerradoAssetGradeEntity> findAllByUserId(Long userId);

    Optional<CerradoAssetGradeEntity> findByUserIdAndTicker(Long userId, String ticker);

    void deleteByUserId(Long userId);
}
