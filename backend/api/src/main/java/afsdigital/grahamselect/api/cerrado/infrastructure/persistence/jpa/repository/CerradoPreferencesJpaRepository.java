package afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities.CerradoPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CerradoPreferencesJpaRepository extends JpaRepository<CerradoPreferencesEntity, Long> {

    Optional<CerradoPreferencesEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
