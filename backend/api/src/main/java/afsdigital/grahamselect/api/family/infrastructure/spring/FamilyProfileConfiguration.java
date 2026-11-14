package afsdigital.grahamselect.api.family.infrastructure.spring;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.usecase.LinkFamilyProfileUseCase;
import afsdigital.grahamselect.common.family.application.usecase.ListFamilyProfilesUseCase;
import afsdigital.grahamselect.common.family.application.usecase.UnlinkFamilyProfileUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FamilyProfileConfiguration {

    @Bean
    public LinkFamilyProfileUseCase linkFamilyProfileUseCase(FamilyProfilePort familyProfilePort) {
        return new LinkFamilyProfileUseCase(familyProfilePort);
    }

    @Bean
    public ListFamilyProfilesUseCase listFamilyProfilesUseCase(FamilyProfilePort familyProfilePort) {
        return new ListFamilyProfilesUseCase(familyProfilePort);
    }

    @Bean
    public UnlinkFamilyProfileUseCase unlinkFamilyProfileUseCase(FamilyProfilePort familyProfilePort) {
        return new UnlinkFamilyProfileUseCase(familyProfilePort);
    }
}

