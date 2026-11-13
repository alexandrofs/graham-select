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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTickerServiceTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    @Mock
    private JpaTradeRepository jpaTradeRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private AdminTickerService adminTickerService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache("market-data")).thenReturn(cache);
    }

    @Test
    @DisplayName("Should list all tickers ordered by ticker ASC when no filters provided")
    void shouldListAllTickersWhenNoFiltersProvided() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity c1 = CompanyEntity.builder()
                .id("id-1")
                .ticker("BBAS3")
                .name("Banco do Brasil")
                .assetClass("Ações")
                .sector("Financeiro")
                .createdAt(now)
                .updatedAt(now)
                .build();

        CompanyEntity c2 = CompanyEntity.builder()
                .id("id-2")
                .ticker("VALE3")
                .name("Vale")
                .assetClass("Ações")
                .sector("Mineração")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(companyJpaRepository.findAllByOrderByTickerAsc()).thenReturn(List.of(c1, c2));

        List<AdminTickerDto> result = adminTickerService.listTickers(null, null);

        assertEquals(2, result.size());
        assertEquals("BBAS3", result.get(0).ticker());
        assertEquals("VALE3", result.get(1).ticker());
        verify(companyJpaRepository).findAllByOrderByTickerAsc();
    }

    @Test
    @DisplayName("Should search tickers when query or assetClass is provided")
    void shouldSearchTickersWhenQueryOrAssetClassProvided() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity c = CompanyEntity.builder()
                .id("id-1")
                .ticker("HGLG11")
                .name("CSHG Logística")
                .assetClass("FIIs")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(companyJpaRepository.searchTickers("HGLG", "FIIs")).thenReturn(List.of(c));

        List<AdminTickerDto> result = adminTickerService.listTickers("HGLG", "FIIs");

        assertEquals(1, result.size());
        assertEquals("HGLG11", result.get(0).ticker());
        assertEquals("FIIs", result.get(0).assetClass());
        verify(companyJpaRepository).searchTickers("HGLG", "FIIs");
    }

    @Test
    @DisplayName("Should create new ticker, sanitize uppercase, default class to Ações and evict cache")
    void shouldCreateNewTickerSuccessfully() {
        CreateAdminTickerRequest request = new CreateAdminTickerRequest(
                "  bova11  ",
                "iShares Ibovespa",
                "ETFs",
                "Índices"
        );

        when(companyJpaRepository.existsByTickerIgnoreCase("BOVA11")).thenReturn(false);
        when(companyJpaRepository.saveAndFlush(any(CompanyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminTickerDto result = adminTickerService.createTicker(request);

        assertNotNull(result);
        assertEquals("BOVA11", result.ticker());
        assertEquals("iShares Ibovespa", result.name());
        assertEquals("ETFs", result.assetClass());
        assertEquals("Índices", result.sector());
        assertNotNull(result.createdAt());
        assertNotNull(result.updatedAt());

        ArgumentCaptor<CompanyEntity> captor = ArgumentCaptor.forClass(CompanyEntity.class);
        verify(companyJpaRepository).saveAndFlush(captor.capture());
        CompanyEntity saved = captor.getValue();
        assertEquals("BOVA11", saved.getTicker());
        assertEquals("ETFs", saved.getAssetClass());

        verify(cache).evict("BOVA11");
        verify(cache).evict("BOVA11F");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when ticker already exists")
    void shouldThrowDuplicateResourceExceptionWhenTickerExists() {
        CreateAdminTickerRequest request = new CreateAdminTickerRequest("PETR4", "Petrobras", "Ações", "Petróleo");
        when(companyJpaRepository.existsByTickerIgnoreCase("PETR4")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> adminTickerService.createTicker(request));
        verify(companyJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ticker is invalid or non-alphanumeric or assetClass is invalid")
    void shouldThrowIllegalArgumentExceptionWhenTickerInvalid() {
        assertThrows(IllegalArgumentException.class, () -> adminTickerService.createTicker(new CreateAdminTickerRequest(" ", "Name", "Ações", null)));
        assertThrows(IllegalArgumentException.class, () -> adminTickerService.createTicker(new CreateAdminTickerRequest("A", "Name", "Ações", null)));
        assertThrows(IllegalArgumentException.class, () -> adminTickerService.createTicker(new CreateAdminTickerRequest("ABCDEFGHIJK", "Name", "Ações", null)));
        assertThrows(IllegalArgumentException.class, () -> adminTickerService.createTicker(new CreateAdminTickerRequest("PETR-4!", "Name", "Ações", null)));
        assertThrows(IllegalArgumentException.class, () -> adminTickerService.createTicker(new CreateAdminTickerRequest("PETR4", "Name", "Cripto", null)));
    }

    @Test
    @DisplayName("Should update existing ticker and evict cache")
    void shouldUpdateExistingTickerSuccessfully() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity entity = CompanyEntity.builder()
                .id("id-123")
                .ticker("SMAL11")
                .name("Small Caps ETF")
                .assetClass("FIIs")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(companyJpaRepository.findById("id-123")).thenReturn(Optional.of(entity));
        when(companyJpaRepository.save(any(CompanyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAdminTickerRequest request = new UpdateAdminTickerRequest("iShares Small Cap", "ETFs", "Small Caps");
        AdminTickerDto result = adminTickerService.updateTicker("id-123", request);

        assertEquals("ETFs", result.assetClass());
        assertEquals("iShares Small Cap", result.name());
        verify(cache).evict("SMAL11");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent ticker")
    void shouldThrowResourceNotFoundWhenUpdatingNonExistentTicker() {
        when(companyJpaRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminTickerService.updateTicker("invalid-id", new UpdateAdminTickerRequest("Name", "Ações", null))
        );
    }

    @Test
    @DisplayName("Should delete ticker and evict cache")
    void shouldDeleteTickerSuccessfully() {
        CompanyEntity entity = CompanyEntity.builder()
                .id("id-to-delete")
                .ticker("XPTO3")
                .build();

        when(companyJpaRepository.findById("id-to-delete")).thenReturn(Optional.of(entity));
        when(jpaTradeRepository.existsByTickerIgnoreCase("XPTO3")).thenReturn(false);

        adminTickerService.deleteTicker("id-to-delete");

        verify(companyJpaRepository).delete(entity);
        verify(companyJpaRepository).flush();
        verify(cache).evict("XPTO3");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when deleting ticker with linked trades")
    void shouldRejectDeleteWhenTradesExist() {
        CompanyEntity entity = CompanyEntity.builder()
                .id("id-with-trades")
                .ticker("PETR4")
                .build();

        when(companyJpaRepository.findById("id-with-trades")).thenReturn(Optional.of(entity));
        when(jpaTradeRepository.existsByTickerIgnoreCase("PETR4")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> adminTickerService.deleteTicker("id-with-trades"));
        verify(companyJpaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow clearing sector when updating ticker with null or blank sector")
    void shouldClearSectorOnUpdate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity entity = CompanyEntity.builder()
                .id("id-sector")
                .ticker("WEGE3")
                .name("WEG")
                .assetClass("Ações")
                .sector("Motores")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(companyJpaRepository.findById("id-sector")).thenReturn(Optional.of(entity));
        when(companyJpaRepository.save(any(CompanyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminTickerDto result = adminTickerService.updateTicker("id-sector", new UpdateAdminTickerRequest("WEG S.A.", "Ações", null));

        assertNull(result.sector());
    }

    @Test
    @DisplayName("Should escape LIKE wildcard characters in query")
    void shouldEscapeLikeWildcardsInQuery() {
        when(companyJpaRepository.searchTickers("100!% !_test", null)).thenReturn(List.of());

        adminTickerService.listTickers("100% _test", null);

        verify(companyJpaRepository).searchTickers("100!% !_test", null);
    }

    @Test
    @DisplayName("Should clear market-data cache entirely")
    void shouldClearMarketDataCache() {
        CacheEvictionResultDto result = adminTickerService.clearMarketDataCache();

        assertNotNull(result);
        assertEquals("Cache de Market Data limpo com sucesso.", result.message());
        assertNotNull(result.timestamp());
        verify(cache).clear();
    }
}
