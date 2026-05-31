package afsdigital.grahamselect.valuation.infrastructure.spring;

import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.application.repository.ValuationRepository;
import afsdigital.grahamselect.valuation.application.service.CompanyLookupService;
import afsdigital.grahamselect.valuation.application.service.IntrinsicValueCalculatorService;
import afsdigital.grahamselect.valuation.application.usecase.CalculationIntrinsicValueUseCase;
import afsdigital.grahamselect.valuation.infrastructure.persistence.CompanyRepositoryImpl;
import afsdigital.grahamselect.valuation.infrastructure.persistence.ValuationRepositoryImpl;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValuationServiceConfiguration {

    @Bean
    public CompanyRepository companyRepository(CompanyJpaRepository companyJpaRepository) {
        return new CompanyRepositoryImpl(companyJpaRepository);
    }

    @Bean
    public ValuationRepository valuationRepository(
            IntrinsicValueJpaRepository intrinsicValueJpaRepository,
            StockPriceJpaRepository stockPriceJpaRepository) {
        return new ValuationRepositoryImpl(intrinsicValueJpaRepository, stockPriceJpaRepository);
    }

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
            ValuationRepository valuationRepository) {
        return new CalculationIntrinsicValueUseCase(
                intrinsicValueCalculatorService,
                companyLookupService,
                valuationRepository);
    }
}
