package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioEvolutionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    private final GetPortfolioEvolutionUseCase getPortfolioEvolutionUseCase;

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDTO> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId
    ) {
        String userId = jwt.getSubject();
        PortfolioSummaryDTO summary = executeWithLogging("summary", userId, tagId,
                () -> tagId == null
                        ? getPortfolioSummaryUseCase.execute(userId)
                        : getPortfolioSummaryUseCase.execute(userId, tagId));
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/custody")
    public ResponseEntity<Map<String, Object>> getCustody(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId
    ) {
        String userId = jwt.getSubject();
        GetCustodyPositionsUseCase.CustodyPositionsResult result = executeWithLogging("custody", userId, tagId,
                () -> getCustodyPositionsUseCase.executeWithMetadata(userId, tagId));

        List<CustodyPositionDTO> positions = result != null && result.positions() != null
                ? result.positions()
                : Collections.emptyList();
        List<String> untaggedTickers = result != null && result.untaggedTickers() != null
                ? result.untaggedTickers()
                : Collections.emptyList();

        // Retorna null quando não há cotação real — evita exibir "Atualizado em HH:mm" com hora falsa
        Instant latestUpdate = positions.stream()
                .map(CustodyPositionDTO::priceUpdatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        // Usa HashMap para permitir valores null no meta (Map.of() não aceita null)
        Map<String, Object> meta = new HashMap<>();
        meta.put("total", positions.size());
        meta.put("priceUpdatedAt", latestUpdate); // null quando todas as posições são CACHE sem data
        meta.put("untaggedAssetsCount", untaggedTickers.size());
        meta.put("untaggedTickers", untaggedTickers);

        Map<String, Object> response = Map.of(
            "data", positions,
            "meta", meta
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evolution")
    public ResponseEntity<PortfolioEvolutionDTO> getEvolution(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId
    ) {
        String userId = jwt.getSubject();
        PortfolioEvolutionDTO evolution = executeWithLogging("evolution", userId, tagId,
                () -> tagId == null
                        ? getPortfolioEvolutionUseCase.execute(userId)
                        : getPortfolioEvolutionUseCase.execute(userId, tagId));
        return ResponseEntity.ok(evolution);
    }

    private <T> T executeWithLogging(String endpoint, String userId, UUID tagId, Supplier<T> action) {
        log.info("Recebida requisição GET /api/v1/portfolios/{} para userId={}, tagId={}",
                endpoint, sanitizeForLog(userId), tagId);
        try {
            return action.get();
        } catch (RuntimeException ex) {
            log.error("Erro ao processar GET /api/v1/portfolios/{} para userId={}, tagId={}: {}",
                    endpoint, sanitizeForLog(userId), tagId, ex.getMessage(), ex);
            throw ex;
        }
    }

    private static String sanitizeForLog(String value) {
        return value == null ? "null" : value.replaceAll("[\\r\\n]", "_");
    }
}
