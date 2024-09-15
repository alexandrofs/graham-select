package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CompanyLookupService {

    private final CompanyRepository companyRepository;

    public Company findOrCreateCompany(String ticker) {

        Company company = companyRepository.findByTicker(ticker);

        if (company == null) {
            company = new Company(ticker);
            company = companyRepository.save(company);
        }

        return company;
    }


}
