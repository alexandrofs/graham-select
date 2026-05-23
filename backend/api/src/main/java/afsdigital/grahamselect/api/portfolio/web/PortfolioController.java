package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDTO> getSummary(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(userId);
        return ResponseEntity.ok(summary);
    }
}
