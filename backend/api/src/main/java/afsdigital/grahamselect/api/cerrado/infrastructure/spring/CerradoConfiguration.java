package afsdigital.grahamselect.api.cerrado.infrastructure.spring;

import afsdigital.grahamselect.common.ai.application.service.AiDiagnosticCacheService;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoPreferencesPort;
import afsdigital.grahamselect.common.cerrado.application.service.CerradoPreferencesService;
import afsdigital.grahamselect.common.cerrado.domain.service.CerradoAllocationEngine;
import afsdigital.grahamselect.common.cerrado.domain.service.CerradoTemplateCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CerradoConfiguration {

    @Bean
    public CerradoAllocationEngine cerradoAllocationEngine() {
        return new CerradoAllocationEngine();
    }

    @Bean
    public CerradoTemplateCatalog cerradoTemplateCatalog() {
        return new CerradoTemplateCatalog();
    }

    @Bean
    public CerradoPreferencesService cerradoPreferencesService(
            CerradoPreferencesPort preferencesPort,
            CerradoAllocationEngine allocationEngine,
            AiDiagnosticCacheService aiDiagnosticCacheService,
            CerradoTemplateCatalog templateCatalog
    ) {
        return new CerradoPreferencesService(
                preferencesPort,
                allocationEngine,
                aiDiagnosticCacheService,
                templateCatalog
        );
    }
}
