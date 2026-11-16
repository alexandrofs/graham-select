package afsdigital.grahamselect.api.dividends.web;

import afsdigital.grahamselect.common.dividends.application.dto.DividendScheduleDTO;
import afsdigital.grahamselect.common.dividends.application.dto.RelevantFactDTO;
import afsdigital.grahamselect.common.dividends.application.usecase.GetDividendScheduleUseCase;
import afsdigital.grahamselect.common.dividends.application.usecase.GetRelevantFactsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/dividends")
@RequiredArgsConstructor
public class DividendController {

    private final GetDividendScheduleUseCase getDividendScheduleUseCase;
    private final GetRelevantFactsUseCase getRelevantFactsUseCase;

    @GetMapping("/schedule")
    public ResponseEntity<List<DividendScheduleDTO>> getSchedule(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean onlyMyTickers,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : null;
        log.info("Recebendo requisição GET /api/v1/dividends/schedule: userId={}, ticker={}, startDate={}, endDate={}, status={}, onlyMyTickers={}",
                userId, sanitizeForLog(ticker), startDate, endDate, sanitizeForLog(status), onlyMyTickers);

        try {
            List<DividendScheduleDTO> schedule = getDividendScheduleUseCase.execute(
                    userId,
                    ticker,
                    startDate,
                    endDate,
                    status,
                    onlyMyTickers
            );
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            log.error("Erro ao consultar agenda de proventos para userId={}, ticker={}: {}",
                    userId, sanitizeForLog(ticker), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/facts")
    public ResponseEntity<List<RelevantFactDTO>> getFacts(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean onlyMyTickers,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = (jwt != null) ? jwt.getSubject() : null;
        log.info("Recebendo requisição GET /api/v1/dividends/facts: userId={}, ticker={}, limit={}, onlyMyTickers={}",
                userId, sanitizeForLog(ticker), limit, onlyMyTickers);

        try {
            List<RelevantFactDTO> facts = getRelevantFactsUseCase.execute(
                    userId,
                    ticker,
                    null,
                    null,
                    limit,
                    onlyMyTickers
            );
            return ResponseEntity.ok(facts);
        } catch (Exception e) {
            log.error("Erro ao consultar fatos relevantes para userId={}, ticker={}: {}",
                    userId, sanitizeForLog(ticker), e.getMessage(), e);
            throw e;
        }
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            sb.append(Character.isISOControl(ch) ? '_' : ch);
        }
        return sb.toString();
    }
}
