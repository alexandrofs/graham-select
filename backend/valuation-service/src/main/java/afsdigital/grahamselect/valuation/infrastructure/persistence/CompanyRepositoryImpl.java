package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public Company findByTicker(String ticker) {
        AtomicReference<Company> company = new AtomicReference<>();
        companyJpaRepository.findByTicker(ticker)
                .ifPresent(companyEntity -> company.set(new Company(companyEntity.getId(), companyEntity.getTicker())));
        return company.get();
    }

    @Override
    public Company save(Company company) {
        CompanyEntity companyEntity = CompanyEntity.builder()
                .id(company.getId())
                .ticker(company.getTicker())
                .build();
        companyJpaRepository.save(companyEntity);
        return company;
    }

}
