package afsdigital.grahamselect.common.family.application.repository;

import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyProfilePort {
    FamilyProfile save(FamilyProfile profile);

    List<FamilyProfile> findActiveByMasterUserId(String masterUserId);

    Optional<FamilyProfile> findActiveByIdAndMasterUserId(UUID id, String masterUserId);

    Optional<FamilyProfile> findByMasterUserIdAndCpfDigits(String masterUserId, String cpfDigits);
}

