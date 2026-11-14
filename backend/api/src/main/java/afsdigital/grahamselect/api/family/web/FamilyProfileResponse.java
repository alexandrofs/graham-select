package afsdigital.grahamselect.api.family.web;

import afsdigital.grahamselect.common.family.domain.CpfValidator;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;

import java.time.LocalDateTime;
import java.util.UUID;

public record FamilyProfileResponse(
        UUID id,
        String name,
        String cpfMasked,
        String relationship,
        boolean active,
        LocalDateTime createdAt
) {
    public static FamilyProfileResponse from(FamilyProfile profile) {
        return new FamilyProfileResponse(
                profile.getId(),
                profile.getName(),
                CpfValidator.mask(profile.getCpfDigits()),
                profile.getRelationship(),
                profile.isActive(),
                profile.getCreatedAt()
        );
    }
}

