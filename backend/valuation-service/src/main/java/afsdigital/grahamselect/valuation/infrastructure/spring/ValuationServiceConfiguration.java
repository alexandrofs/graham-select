package afsdigital.grahamselect.valuation.infrastructure.spring;

import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.application.repository.IntrinsicValueRepository;
import afsdigital.grahamselect.valuation.application.service.CompanyLookupService;
import afsdigital.grahamselect.valuation.application.service.IntrinsicValueCalculatorService;
import afsdigital.grahamselect.valuation.application.usecase.CalculationIntrinsicValueUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "afsdigital.grahamselect.valuation.infrastructure",
        "afsdigital.grahamselect.valuation.application"
})
public class ValuationServiceConfiguration {

    @Bean
    public IntrinsicValueCalculatorService intrinsicValueCalculatorService() {
        return new IntrinsicValueCalculatorService();
    }

    @Bean
    public CompanyLookupService companyLookupService(CompanyRepository companyRepository) {
        return new CompanyLookupService(companyRepository);
    }

    @Bean
    public CalculationIntrinsicValueUseCase calculationIntrinsicValueUseCase(
            IntrinsicValueCalculatorService intrinsicValueCalculatorService,
            CompanyLookupService companyLookupService,
            IntrinsicValueRepository intrinsicValueRepository) {
        return new CalculationIntrinsicValueUseCase(
                intrinsicValueCalculatorService,
                companyLookupService,
                intrinsicValueRepository);
    }
}
