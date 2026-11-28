package afsdigital.grahamselect.common.cerrado.application.repository;

import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetGrade;

import java.util.List;
import java.util.Optional;

public interface CerradoAssetGradePort {

    List<CerradoAssetGrade> findAllByUserId(Long userId);

    Optional<CerradoAssetGrade> findByUserIdAndTicker(Long userId, String ticker);

    CerradoAssetGrade save(CerradoAssetGrade assetGrade);

    void deleteByUserId(Long userId);
}
