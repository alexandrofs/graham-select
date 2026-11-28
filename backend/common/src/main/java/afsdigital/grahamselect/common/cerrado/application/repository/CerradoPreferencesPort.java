package afsdigital.grahamselect.common.cerrado.application.repository;

import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;

import java.util.Optional;

public interface CerradoPreferencesPort {

    Optional<CerradoPreferences> findByUserId(Long userId);

    CerradoPreferences save(CerradoPreferences preferences);

    void deleteByUserId(Long userId);
}
