package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;

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
        
        Instant latestUpdate = positions.stream()
                .map(CustodyPositionDTO::priceUpdatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(Instant.now());

        Map<String, Object> response = Map.of(
            "data", positions,
            "meta", Map.of(
                "total", positions.size(),
                "priceUpdatedAt", latestUpdate
            )
        );
        return ResponseEntity.ok(response);
    }
}
