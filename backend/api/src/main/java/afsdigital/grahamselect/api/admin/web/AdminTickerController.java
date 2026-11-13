package afsdigital.grahamselect.api.admin.web;

import afsdigital.grahamselect.api.admin.application.service.AdminTickerService;
import afsdigital.grahamselect.api.admin.dto.AdminTickerDto;
import afsdigital.grahamselect.api.admin.dto.CacheEvictionResultDto;
import afsdigital.grahamselect.api.admin.dto.CreateAdminTickerRequest;
import afsdigital.grahamselect.api.admin.dto.UpdateAdminTickerRequest;
import afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/tickers")
@RequiredArgsConstructor
@Slf4j
public class AdminTickerController {

    private final AdminTickerService adminTickerService;

    @GetMapping
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> listTickers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String assetClass,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Request to list master data tickers received from principal: {} (query='{}', assetClass='{}')", principal, query, assetClass);
        List<AdminTickerDto> tickers = adminTickerService.listTickers(query, assetClass);
        return ResponseEntity.ok(Map.of("data", tickers));
    }

    @PostMapping
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> createTicker(
            @Valid @RequestBody CreateAdminTickerRequest request,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Request to create master data ticker received from principal: {} with payload: {}", principal, request);
        AdminTickerDto created = adminTickerService.createTicker(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", created));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> updateTicker(
            @PathVariable String id,
            @Valid @RequestBody UpdateAdminTickerRequest request,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Request to update master data ticker ID: {} received from principal: {} with payload: {}", id, principal, request);
        AdminTickerDto updated = adminTickerService.updateTicker(id, request);
        return ResponseEntity.ok(Map.of("data", updated));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<Void> deleteTicker(
            @PathVariable String id,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Request to delete master data ticker ID: {} received from principal: {}", id, principal);
        adminTickerService.deleteTicker(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/clear")
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> clearCache(Authentication authentication) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Request to clear market-data cache received from principal: {}", principal);
        CacheEvictionResultDto result = adminTickerService.clearMarketDataCache();
        return ResponseEntity.ok(Map.of("data", result));
    }

    @PostMapping("/{ticker}/cache/evict")
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> evictTickerCache(
            @PathVariable String ticker,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Request to evict ticker {} from market-data cache received from principal: {}", ticker, principal);
        CacheEvictionResultDto result = adminTickerService.evictMarketDataCache(ticker);
        return ResponseEntity.ok(Map.of("data", result));
    }
}
