package afsdigital.grahamselect.api.tax.infrastructure.spring;

import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxAssetDeclarationReportUseCase;
import afsdigital.grahamselect.common.tax.application.usecase.GetTaxAvailableYearsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaxConfiguration {

    @Bean
    public GetTaxAssetDeclarationReportUseCase getTaxAssetDeclarationReportUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository
    ) {
        return new GetTaxAssetDeclarationReportUseCase(tradePort, companyRepository);
    }

    @Bean
    public GetTaxAvailableYearsUseCase getTaxAvailableYearsUseCase(TradePort tradePort) {
        return new GetTaxAvailableYearsUseCase(tradePort);
    }

}
