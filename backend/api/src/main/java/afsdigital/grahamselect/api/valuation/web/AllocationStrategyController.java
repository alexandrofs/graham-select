package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequirePremium;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.usecase.GetAllocationGoalsUseCase;
import afsdigital.grahamselect.valuation.application.usecase.SaveAllocationGoalsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/allocation-strategy")
@RequiredArgsConstructor
@Slf4j
@RequirePremium
public class AllocationStrategyController {

    private final SaveAllocationGoalsUseCase saveAllocationGoalsUseCase;
    private final GetAllocationGoalsUseCase getAllocationGoalsUseCase;

    @PostMapping
    public ResponseEntity<Void> saveAllocationGoals(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId,
            @RequestBody List<AllocationGoalDto> goals
    ) {
        String userId = jwt.getSubject();
        log.info("Received request to save allocation strategy for user {}, tagId={}", sanitize(userId), tagId);
        if (tagId == null) {
            saveAllocationGoalsUseCase.execute(userId, goals);
        } else {
            saveAllocationGoalsUseCase.execute(userId, tagId, goals);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AllocationGoalDto>> getAllocationGoals(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId
    ) {
        String userId = jwt.getSubject();
        log.info("Received request to get allocation strategy for user {}, tagId={}", sanitize(userId), tagId);
        List<AllocationGoalDto> goals = tagId == null
                ? getAllocationGoalsUseCase.execute(userId)
                : getAllocationGoalsUseCase.execute(userId, tagId);
        return ResponseEntity.ok(goals);
    }

    private static String sanitize(String value) {
        return value == null ? "null" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
