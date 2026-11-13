package afsdigital.grahamselect.api.admin.application.service;

import afsdigital.grahamselect.api.admin.application.service.exceptions.DuplicateResourceException;
import afsdigital.grahamselect.api.admin.application.service.exceptions.ResourceNotFoundException;
import afsdigital.grahamselect.api.admin.dto.AdminTickerDto;
import afsdigital.grahamselect.api.admin.dto.CacheEvictionResultDto;
import afsdigital.grahamselect.api.admin.dto.CreateAdminTickerRequest;
import afsdigital.grahamselect.api.admin.dto.UpdateAdminTickerRequest;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTickerService {

    private static final Map<String, String> ALLOWED_ASSET_CLASSES = Map.of(
            "ações", "Ações",
            "acoes", "Ações",
            "fiis", "FIIs",
            "bdrs", "BDRs",
            "etfs", "ETFs",
            "renda fixa", "Renda Fixa",
            "outros", "Outros"
    );

    private final CompanyJpaRepository companyJpaRepository;
    private final JpaTradeRepository jpaTradeRepository;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public List<AdminTickerDto> listTickers(String query, String assetClass) {
        log.info("Listing master data tickers with query: '{}' and assetClass: '{}'", query, assetClass);
        String sanitizedQuery = (query != null && !query.trim().isBlank()) ? escapeLikeWildcards(query.trim()) : null;
        String sanitizedAssetClass = (assetClass != null && !assetClass.trim().isBlank() && !"Todos".equalsIgnoreCase(assetClass.trim()))
                ? assetClass.trim()
                : null;

        List<CompanyEntity> entities;
        if (sanitizedQuery == null && sanitizedAssetClass == null) {
            entities = companyJpaRepository.findAllByOrderByTickerAsc();
        } else {
            entities = companyJpaRepository.searchTickers(sanitizedQuery, sanitizedAssetClass);
        }

        return entities.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AdminTickerDto createTicker(CreateAdminTickerRequest request) {
        log.info("Creating new master data ticker: {}", request != null ? request.ticker() : null);
        if (request == null || request.ticker() == null || request.ticker().trim().isBlank()) {
            throw new IllegalArgumentException("O ticker é obrigatório.");
        }

        String sanitizedTicker = request.ticker().trim().toUpperCase();
        if (!sanitizedTicker.matches("^[A-Z0-9]{2,10}$")) {
            throw new IllegalArgumentException("O ticker deve conter entre 2 e 10 caracteres alfanuméricos.");
        }

        if (companyJpaRepository.existsByTickerIgnoreCase(sanitizedTicker)) {
            throw new DuplicateResourceException("Ticker já cadastrado no dicionário mestre: " + sanitizedTicker);
        }

        String assetClass = normalizeAndValidateAssetClass(request.assetClass(), "Ações");

        String name = (request.name() != null && !request.name().trim().isBlank())
                ? request.name().trim()
                : sanitizedTicker;

        String sector = (request.sector() != null && !request.sector().trim().isBlank())
                ? request.sector().trim()
                : null;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity entity = CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker(sanitizedTicker)
                .name(name)
                .assetClass(assetClass)
                .sector(sector)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CompanyEntity saved;
        try {
            saved = companyJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation creating ticker {}: {}", sanitizedTicker, ex.getMessage(), ex);
            throw new DuplicateResourceException("Ticker já cadastrado no dicionário mestre: " + sanitizedTicker);
        }

        evictMarketDataCache(sanitizedTicker);

        log.info("Successfully created master data ticker {} with ID {}", sanitizedTicker, saved.getId());
        return toDto(saved);
    }

    @Transactional
    public AdminTickerDto updateTicker(String id, UpdateAdminTickerRequest request) {
        log.info("Updating master data ticker ID: {}", id);
        CompanyEntity entity = companyJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker não encontrado com ID: " + id));

        if (request != null) {
            if (request.name() != null && !request.name().trim().isBlank()) {
                entity.setName(request.name().trim());
            }
            if (request.assetClass() != null && !request.assetClass().trim().isBlank()) {
                entity.setAssetClass(normalizeAndValidateAssetClass(request.assetClass(), entity.getAssetClass()));
            }
            entity.setSector((request.sector() == null || request.sector().trim().isBlank())
                    ? null
                    : request.sector().trim());
        }
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        CompanyEntity saved = companyJpaRepository.save(entity);
        evictMarketDataCache(saved.getTicker());

        log.info("Successfully updated master data ticker {} (ID: {})", saved.getTicker(), id);
        return toDto(saved);
    }

    @Transactional
    public void deleteTicker(String id) {
        log.info("Deleting master data ticker ID: {}", id);
        CompanyEntity entity = companyJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker não encontrado com ID: " + id));

        String ticker = entity.getTicker();
        if (ticker != null && jpaTradeRepository.existsByTickerIgnoreCase(ticker)) {
            throw new DuplicateResourceException("Não é possível remover o ticker " + ticker + " pois existem transações vinculadas.");
        }

        try {
            companyJpaRepository.delete(entity);
            companyJpaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.error("Referential integrity violation deleting ticker {} (ID: {}): {}", ticker, id, ex.getMessage(), ex);
            throw new DuplicateResourceException("Não é possível remover o ticker " + ticker + " pois existem cotações ou avaliações vinculadas.");
        }

        evictMarketDataCache(ticker);
        log.info("Successfully deleted master data ticker {} (ID: {})", ticker, id);
    }

    public CacheEvictionResultDto clearMarketDataCache() {
        log.info("Clearing market-data cache by administrator request");
        Cache cache = cacheManager.getCache("market-data");
        if (cache != null) {
            cache.clear();
        }
        log.info("Market data cache cleared by administrator");
        return new CacheEvictionResultDto("Cache de Market Data limpo com sucesso.", OffsetDateTime.now(ZoneOffset.UTC));
    }

    public CacheEvictionResultDto evictMarketDataCache(String ticker) {
        log.info("Evicting ticker {} from market-data cache", ticker);
        if (ticker != null && !ticker.trim().isBlank()) {
            String cleanTicker = ticker.trim().toUpperCase();
            Cache cache = cacheManager.getCache("market-data");
            if (cache != null) {
                cache.evict(cleanTicker);
                cache.evict(cleanTicker + "F");
                if (!cleanTicker.equals(ticker)) {
                    cache.evict(ticker);
                }
            }
            log.info("Evicted ticker {} from market-data cache", cleanTicker);
            return new CacheEvictionResultDto("Ticker " + cleanTicker + " removido do cache de Market Data.", OffsetDateTime.now(ZoneOffset.UTC));
        }
        return new CacheEvictionResultDto("Nenhum ticker especificado para evicção.", OffsetDateTime.now(ZoneOffset.UTC));
    }

    private String normalizeAndValidateAssetClass(String rawAssetClass, String defaultValue) {
        if (rawAssetClass == null || rawAssetClass.trim().isBlank()) {
            return defaultValue;
        }
        String key = rawAssetClass.trim().toLowerCase();
        String canonical = ALLOWED_ASSET_CLASSES.get(key);
        if (canonical == null) {
            throw new IllegalArgumentException("Classe de ativo inválida. Valores permitidos: Ações, FIIs, BDRs, ETFs, Renda Fixa, Outros.");
        }
        return canonical;
    }

    private String escapeLikeWildcards(String input) {
        return input
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private AdminTickerDto toDto(CompanyEntity entity) {
        OffsetDateTime createdAtUtc = entity.getCreatedAt() != null
                ? entity.getCreatedAt().atOffset(ZoneOffset.UTC)
                : null;
        OffsetDateTime updatedAtUtc = entity.getUpdatedAt() != null
                ? entity.getUpdatedAt().atOffset(ZoneOffset.UTC)
                : null;

        return new AdminTickerDto(
                entity.getId(),
                entity.getTicker(),
                entity.getName(),
                entity.getAssetClass(),
                entity.getSector(),
                createdAtUtc,
                updatedAtUtc
        );
    }
}
