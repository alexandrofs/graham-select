package afsdigital.grahamselect.api.family.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.entities.FamilyProfileEntity;
import afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.repositories.JpaFamilyProfileRepository;
import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.DuplicateFamilyProfileException;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FamilyProfileJpaAdapter implements FamilyProfilePort {

    private final JpaFamilyProfileRepository repository;

    @Override
    public FamilyProfile save(FamilyProfile profile) {
        FamilyProfileEntity entity = FamilyProfileEntity.builder()
                .id(profile.getId())
                .masterUserId(profile.getMasterUserId())
                .cpfDigits(profile.getCpfDigits())
                .name(profile.getName())
                .relationship(profile.getRelationship())
                .active(profile.isActive())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();

        try {
            return toDomain(repository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateFamilyProfileException("CPF já vinculado à conta matriz.");
        }
    }

    @Override
    public List<FamilyProfile> findActiveByMasterUserId(String masterUserId) {
        return repository.findAllByMasterUserIdAndActiveTrueOrderByCreatedAtAsc(masterUserId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<FamilyProfile> findActiveByIdAndMasterUserId(UUID id, String masterUserId) {
        return repository.findByIdAndMasterUserIdAndActiveTrue(id, masterUserId)
                .map(this::toDomain);
    }

    @Override
    public Optional<FamilyProfile> findByMasterUserIdAndCpfDigits(String masterUserId, String cpfDigits) {
        return repository.findByMasterUserIdAndCpfDigits(masterUserId, cpfDigits)
                .map(this::toDomain);
    }

    private FamilyProfile toDomain(FamilyProfileEntity entity) {
        return FamilyProfile.builder()
                .id(entity.getId())
                .masterUserId(entity.getMasterUserId())
                .cpfDigits(entity.getCpfDigits())
                .name(entity.getName())
                .relationship(entity.getRelationship())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

