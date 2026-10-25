package afsdigital.grahamselect.api.admin.web;

import afsdigital.grahamselect.api.admin.application.dto.AdminObservabilityDto;
import afsdigital.grahamselect.api.admin.application.service.AdminObservabilityService;
import afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/observability")
@RequiredArgsConstructor
@Slf4j
public class AdminObservabilityController {

    private final AdminObservabilityService adminObservabilityService;

    @GetMapping
    @RequireAdmin
    public ResponseEntity<Map<String, Object>> getObservabilityMetrics(Authentication authentication) {
        String principalName = authentication != null ? authentication.getName() : "anonymous";
        log.info("Received request to get admin observability metrics from principal {}", principalName);
        try {
            AdminObservabilityDto metrics = adminObservabilityService.getObservabilityMetrics();
            return ResponseEntity.ok(Map.of("data", metrics));
        } catch (Exception ex) {
            log.error("Error processing admin observability metrics request for principal {}", principalName, ex);
            throw ex;
        }
    }
}
