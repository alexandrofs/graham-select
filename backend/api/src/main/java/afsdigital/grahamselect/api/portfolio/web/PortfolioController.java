package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioEvolutionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    private final GetPortfolioEvolutionUseCase getPortfolioEvolutionUseCase;

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDTO> getSummary(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/custody")
    public ResponseEntity<Map<String, Object>> getCustody(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<CustodyPositionDTO> positions = getCustodyPositionsUseCase.execute(userId);
        
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

        Map<String, Object> response = Map.of(
            "data", positions,
            "meta", meta
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evolution")
    public ResponseEntity<PortfolioEvolutionDTO> getEvolution(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        PortfolioEvolutionDTO evolution = getPortfolioEvolutionUseCase.execute(userId);
        return ResponseEntity.ok(evolution);
    }
}
