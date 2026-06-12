package afsdigital.grahamselect.valuation.infrastructure.spring;

import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.application.repository.PortfolioSnapshotPort;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.application.repository.ValuationRepository;
import afsdigital.grahamselect.valuation.application.service.CompanyLookupService;
import afsdigital.grahamselect.valuation.application.service.IntrinsicValueCalculatorService;
import afsdigital.grahamselect.valuation.application.usecase.CalculationIntrinsicValueUseCase;
import afsdigital.grahamselect.valuation.application.usecase.GenerateGrahamRecommendationsUseCase;
import afsdigital.grahamselect.valuation.infrastructure.persistence.AllocationGoalReadAdapter;
import afsdigital.grahamselect.valuation.infrastructure.persistence.CompanyRepositoryImpl;
import afsdigital.grahamselect.valuation.infrastructure.persistence.GrahamRecommendationRepositoryImpl;
import afsdigital.grahamselect.valuation.infrastructure.persistence.PortfolioSnapshotJpaAdapter;
import afsdigital.grahamselect.valuation.infrastructure.persistence.RankingReadAdapter;
import afsdigital.grahamselect.valuation.infrastructure.persistence.ValuationRepositoryImpl;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.AllocationGoalReadJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
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

    @Bean
    public GrahamRecommendationPort grahamRecommendationPort(GrahamRecommendationJpaRepository grahamRecommendationJpaRepository) {
        return new GrahamRecommendationRepositoryImpl(grahamRecommendationJpaRepository);
    }

    @Bean
    public PortfolioSnapshotPort portfolioSnapshotPort() {
        return new PortfolioSnapshotJpaAdapter();
    }

    @Bean
    public AllocationGoalPort allocationGoalPort(AllocationGoalReadJpaRepository allocationGoalReadJpaRepository) {
        return new AllocationGoalReadAdapter(allocationGoalReadJpaRepository);
    }

    @Bean
    public RankingRepository rankingRepository() {
        return new RankingReadAdapter();
    }

    @Bean
    public GenerateGrahamRecommendationsUseCase generateGrahamRecommendationsUseCase(
            AllocationGoalPort allocationGoalPort,
            RankingRepository rankingRepository,
            PortfolioSnapshotPort portfolioSnapshotPort,
            GrahamRecommendationPort grahamRecommendationPort) {
        return new GenerateGrahamRecommendationsUseCase(
                allocationGoalPort,
                rankingRepository,
                portfolioSnapshotPort,
                grahamRecommendationPort
        );
    }
}

