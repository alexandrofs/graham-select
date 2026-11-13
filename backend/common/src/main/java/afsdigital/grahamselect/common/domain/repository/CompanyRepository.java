package afsdigital.grahamselect.common.domain.repository;

import afsdigital.grahamselect.common.domain.entities.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {

    Company findByTicker(String ticker);
    Company save(Company company);
    List<Company> findAll();
    Optional<Company> findById(String id);
    void deleteById(String id);
    boolean existsByTicker(String ticker);

}
