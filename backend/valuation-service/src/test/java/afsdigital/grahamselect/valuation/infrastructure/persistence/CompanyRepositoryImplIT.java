package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CompanyRepositoryImplIT extends BaseRepositoryIT {

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @AfterEach
    void tearDown() {
        companyJpaRepository.deleteAll();
    }

    @Test
    void deveSalvarERecuperarCompanyPorTicker() {
        CompanyRepositoryImpl companyRepository = new CompanyRepositoryImpl(companyJpaRepository);
        String id = UUID.randomUUID().toString();
        Company company = new Company(id, "PETR4");
        companyRepository.save(company);

        Company found = companyRepository.findByTicker("PETR4");
        assertThat(found).isNotNull();
        assertThat(found.getTicker()).isEqualTo("PETR4");
        assertThat(found.getId()).isEqualTo(id);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    @EntityScan(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    static class Config {
        @Bean
        public CompanyRepositoryImpl companyRepositoryImpl(CompanyJpaRepository repo) {
            return new CompanyRepositoryImpl(repo);
        }
    }
}
