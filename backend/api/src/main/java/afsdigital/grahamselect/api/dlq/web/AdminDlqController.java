package afsdigital.grahamselect.api.dlq.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin;
import afsdigital.grahamselect.api.dlq.application.dto.DlqBatchReprocessRequest;
import afsdigital.grahamselect.api.dlq.application.dto.DlqBatchReprocessResultDto;
import afsdigital.grahamselect.api.dlq.application.dto.DlqMessageDto;
import afsdigital.grahamselect.api.dlq.application.dto.DlqReprocessResultDto;
import afsdigital.grahamselect.api.dlq.application.service.DlqManagementService;
import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dlq")
@RequiredArgsConstructor
@Slf4j
public class AdminDlqController {

    private final DlqManagementService dlqManagementService;

    @GetMapping
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> getPendingFailures(
            @RequestParam(required = false) DlqMessageStatus status,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Requisição para listar falhas DLQ recebida do principal: {} com status: {}", principal, status);
        try {
            List<DlqMessageDto> failures = dlqManagementService.getPendingFailures(status);
            return ResponseEntity.ok(Map.of("data", failures));
        } catch (Exception ex) {
            log.error("Erro ao listar falhas DLQ para o principal {}: {}", principal, ex.getMessage(), ex);
            throw ex;
        }
    }

    @PostMapping("/{id}/reprocess")
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> reprocess(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Requisição para reprocessar falha DLQ id: {} recebida do principal: {}", id, principal);
        try {
            DlqReprocessResultDto result = dlqManagementService.reprocess(id);
            return ResponseEntity.ok(Map.of("data", result));
        } catch (Exception ex) {
            log.error("Erro ao reprocessar falha DLQ id {} para o principal {}: {}", id, principal, ex.getMessage(), ex);
            throw ex;
        }
    }

    @PostMapping("/reprocess-batch")
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> reprocessBatch(
            @RequestBody(required = false) DlqBatchReprocessRequest request,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        log.info("Requisição para reprocessamento em lote recebida do principal: {} com payload: {}", principal, request);
        try {
            List<Long> ids = request != null ? request.ids() : null;
            DlqBatchReprocessResultDto result = dlqManagementService.reprocessBatch(ids);
            return ResponseEntity.ok(Map.of("data", result));
        } catch (Exception ex) {
            log.error("Erro ao reprocessar lote DLQ para o principal {}: {}", principal, ex.getMessage(), ex);
            throw ex;
        }
    }
}
