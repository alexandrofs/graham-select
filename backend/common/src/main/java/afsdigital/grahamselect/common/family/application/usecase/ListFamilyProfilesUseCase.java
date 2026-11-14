package afsdigital.grahamselect.common.family.application.usecase;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;

import java.util.List;

public class ListFamilyProfilesUseCase {

    private final FamilyProfilePort familyProfilePort;

    public ListFamilyProfilesUseCase(FamilyProfilePort familyProfilePort) {
        this.familyProfilePort = familyProfilePort;
    }

    public List<FamilyProfile> execute(String masterUserId) {
        if (masterUserId == null || masterUserId.isBlank()) {
            throw new IllegalArgumentException("masterUserId é obrigatório.");
        }
        return familyProfilePort.findActiveByMasterUserId(masterUserId);
    }
}

