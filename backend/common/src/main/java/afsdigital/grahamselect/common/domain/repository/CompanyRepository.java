package afsdigital.grahamselect.common.domain.repository;

import afsdigital.grahamselect.common.domain.entities.Company;

public interface CompanyRepository {

    Company findByTicker(String ticker);
    Company save(Company company);

}
