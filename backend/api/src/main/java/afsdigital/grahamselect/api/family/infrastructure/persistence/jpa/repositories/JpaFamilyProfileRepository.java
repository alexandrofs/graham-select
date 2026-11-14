package afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.entities.FamilyProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFamilyProfileRepository extends JpaRepository<FamilyProfileEntity, UUID> {

    List<FamilyProfileEntity> findAllByMasterUserIdAndActiveTrueOrderByCreatedAtAsc(String masterUserId);

    Optional<FamilyProfileEntity> findByIdAndMasterUserIdAndActiveTrue(UUID id, String masterUserId);

    Optional<FamilyProfileEntity> findByMasterUserIdAndCpfDigits(String masterUserId, String cpfDigits);
}

