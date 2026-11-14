package afsdigital.grahamselect.common.family.application.usecase;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class UnlinkFamilyProfileUseCase {

    private final FamilyProfilePort familyProfilePort;

    public UnlinkFamilyProfileUseCase(FamilyProfilePort familyProfilePort) {
        this.familyProfilePort = familyProfilePort;
    }

    public void execute(UUID profileId, String masterUserId) {
        if (profileId == null) {
            throw new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
        }
        if (masterUserId == null || masterUserId.isBlank()) {
            throw new IllegalArgumentException("masterUserId é obrigatório.");
        }

        FamilyProfile profile = familyProfilePort.findActiveByIdAndMasterUserId(profileId, masterUserId)
                .orElseThrow(() -> new FamilyProfileNotFoundException("Perfil familiar não encontrado."));

        profile.setActive(false);
        profile.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        familyProfilePort.save(profile);
    }
}

