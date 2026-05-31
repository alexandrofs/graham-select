package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyJpaAdapter implements CompanyRepository {

    private final CompanyJpaRepository repository;

    @Override
    public Company findByTicker(String ticker) {
        return repository.findByTicker(ticker)
                .map(entity -> new Company(entity.getId(), entity.getTicker()))
                .orElse(null);
    }

    @Override
    public Company save(Company company) {
        // Not needed for this story, but implementing for completeness if required by interface
        return company;
    }
}
