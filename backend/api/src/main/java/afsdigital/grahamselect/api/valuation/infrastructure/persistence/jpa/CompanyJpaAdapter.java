package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyJpaAdapter implements CompanyRepository {

    private final CompanyJpaRepository repository;

    @Override
    public Company findByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        return repository.findByTicker(ticker.trim())
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
        CompanyEntity entity = CompanyEntity.builder()
                .id(id)
                .ticker(company.getTicker())
                .name(company.getName())
                .assetClass(company.getAssetClass())
                .sector(company.getSector())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
        CompanyEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Company> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Company> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        if (id != null && !id.isBlank()) {
            repository.deleteById(id);
        }
    }

    @Override
    public boolean existsByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        return repository.existsByTickerIgnoreCase(ticker.trim());
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

