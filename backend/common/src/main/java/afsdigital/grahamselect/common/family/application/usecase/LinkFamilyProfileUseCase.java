package afsdigital.grahamselect.common.family.application.usecase;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.DuplicateFamilyProfileException;
import afsdigital.grahamselect.common.family.domain.CpfValidator;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public class LinkFamilyProfileUseCase {

    private final FamilyProfilePort familyProfilePort;

    public LinkFamilyProfileUseCase(FamilyProfilePort familyProfilePort) {
        this.familyProfilePort = familyProfilePort;
    }

    public FamilyProfile execute(String masterUserId, String rawCpf, String name, String relationship) {
        if (masterUserId == null || masterUserId.isBlank()) {
            throw new IllegalArgumentException("masterUserId é obrigatório.");
        }
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.length() < 2 || normalizedName.length() > 120) {
            throw new IllegalArgumentException("Nome deve ter entre 2 e 120 caracteres.");
        }
        String normalizedRelationship = relationship == null || relationship.isBlank()
                ? null
                : relationship.trim();
        if (normalizedRelationship != null && normalizedRelationship.length() > 50) {
            throw new IllegalArgumentException("Parentesco deve ter no máximo 50 caracteres.");
        }

        String cpfDigits = CpfValidator.validateAndSanitize(rawCpf);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Optional<FamilyProfile> existing = familyProfilePort.findByMasterUserIdAndCpfDigits(masterUserId, cpfDigits);
        if (existing.isPresent()) {
            FamilyProfile profile = existing.get();
            if (profile.isActive()) {
                throw new DuplicateFamilyProfileException("CPF já vinculado à conta matriz.");
            }
            profile.setActive(true);
            profile.setName(normalizedName);
            profile.setRelationship(normalizedRelationship);
            profile.setUpdatedAt(now);
            return familyProfilePort.save(profile);
        }

        FamilyProfile newProfile = FamilyProfile.builder()
                .id(UUID.randomUUID())
                .masterUserId(masterUserId)
                .cpfDigits(cpfDigits)
                .name(normalizedName)
                .relationship(normalizedRelationship)
                .active(true)
                .createdAt(now)
                .updatedAt(null)
                .build();

        return familyProfilePort.save(newProfile);
    }
}

