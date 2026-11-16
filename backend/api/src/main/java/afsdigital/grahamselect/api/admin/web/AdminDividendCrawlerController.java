package afsdigital.grahamselect.api.admin.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin;
import afsdigital.grahamselect.common.dividends.application.dto.CorporateEventSyncSummaryDTO;
import afsdigital.grahamselect.common.dividends.application.usecase.SyncCorporateEventsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/crawler/dividends")
@RequiredArgsConstructor
public class AdminDividendCrawlerController {

    private final SyncCorporateEventsUseCase syncCorporateEventsUseCase;

    @PostMapping("/sync")
    @RequireAdmin
    public ResponseEntity<CorporateEventSyncSummaryDTO> triggerSync(Authentication authentication) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Recebendo requisição POST /api/v1/admin/crawler/dividends/sync disparada por principal: {}", principal);

        try {
            CorporateEventSyncSummaryDTO summary = syncCorporateEventsUseCase.execute();
            log.info("Sincronização manual acionada com sucesso por principal: {}. Resumo: {}", principal, summary);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Erro na sincronização manual de eventos corporativos solicitada por {}: {}",
                    principal, e.getMessage(), e);
            throw e;
        }
    }
}
