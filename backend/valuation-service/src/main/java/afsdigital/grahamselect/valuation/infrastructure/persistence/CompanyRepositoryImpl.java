package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public Company findByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        return companyJpaRepository.findByTicker(ticker)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public Company save(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null");
        }
        String id = (company.getId() != null && !company.getId().isBlank())
                ? company.getId()
                : UUID.randomUUID().toString();
        CompanyEntity existing = companyJpaRepository.findById(id).orElse(null);
        CompanyEntity companyEntity = CompanyEntity.builder()
                .id(id)
                .ticker(company.getTicker())
                .name(company.getName() != null ? company.getName() : (existing != null ? existing.getName() : null))
                .assetClass(company.getAssetClass() != null ? company.getAssetClass() : (existing != null ? existing.getAssetClass() : null))
                .sector(company.getSector() != null ? company.getSector() : (existing != null ? existing.getSector() : null))
                .createdAt(company.getCreatedAt() != null ? company.getCreatedAt() : (existing != null ? existing.getCreatedAt() : null))
                .updatedAt(company.getUpdatedAt())
                .build();
        CompanyEntity saved = companyJpaRepository.save(companyEntity);
        return toDomain(saved);
    }

    @Override
    public List<Company> findAll() {
        return companyJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Company> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return companyJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        if (id != null && !id.isBlank()) {
            companyJpaRepository.deleteById(id);
        }
    }

    @Override
    public boolean existsByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        return companyJpaRepository.findByTicker(ticker).isPresent();
    }

    private Company toDomain(CompanyEntity entity) {
        return new Company(
                entity.getId(),
                entity.getTicker(),
                entity.getName(),
                entity.getAssetClass(),
                entity.getSector(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
