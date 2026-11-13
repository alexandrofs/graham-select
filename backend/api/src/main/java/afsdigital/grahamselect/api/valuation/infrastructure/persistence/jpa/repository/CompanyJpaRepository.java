package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, String> {

    Optional<CompanyEntity> findByTicker(String ticker);

    boolean existsByTickerIgnoreCase(String ticker);

    List<CompanyEntity> findAllByOrderByTickerAsc();

    @Query("SELECT DISTINCT c.ticker FROM CompanyEntity c WHERE c.ticker IS NOT NULL")
    List<String> findAllTickers();

    @Query("SELECT c FROM CompanyEntity c WHERE " +
            "(:assetClass IS NULL OR LOWER(c.assetClass) = LOWER(:assetClass)) AND " +
            "(:query IS NULL OR LOWER(c.ticker) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!' " +
            "OR (c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!')) " +
            "ORDER BY c.ticker ASC")
    List<CompanyEntity> searchTickers(@Param("query") String query, @Param("assetClass") String assetClass);
}
