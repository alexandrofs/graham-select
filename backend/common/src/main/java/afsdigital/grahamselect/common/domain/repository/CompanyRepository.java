package afsdigital.grahamselect.common.domain.repository;

import afsdigital.grahamselect.common.domain.entities.Company;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {

    Company findByTicker(String ticker);
    default List<Company> findAllByTickerIn(Collection<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return List.of();
        }
        return tickers.stream()
                .map(this::findByTicker)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
    Company save(Company company);
    List<Company> findAll();
    Optional<Company> findById(String id);
    void deleteById(String id);
    boolean existsByTicker(String ticker);

}
