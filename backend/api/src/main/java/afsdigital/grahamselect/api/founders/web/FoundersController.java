package afsdigital.grahamselect.api.founders.web;

import afsdigital.grahamselect.api.founders.web.dto.FounderSlotsResponseDto;
import afsdigital.grahamselect.common.founders.application.service.FounderQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/founders")
@RequiredArgsConstructor
@Slf4j
public class FoundersController {

    private final FounderQuotaService founderQuotaService;

    @GetMapping("/slots")
    public ResponseEntity<FounderSlotsResponseDto> getFounderSlots() {
        log.info("Received request GET /api/v1/founders/slots");
        try {
            var status = founderQuotaService.getSlotsStatus();
            return ResponseEntity.ok(FounderSlotsResponseDto.from(status));
        } catch (Exception ex) {
            log.error("Error processing GET /api/v1/founders/slots", ex);
            throw ex;
        }
    }
}
