package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequirePremium;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.usecase.GetAllocationGoalsUseCase;
import afsdigital.grahamselect.valuation.application.usecase.SaveAllocationGoalsUseCase;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.AllocationGoalValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestBody List<AllocationGoalDto> goals
    ) {
        String userId = jwt.getSubject();
        log.info("Received request to save allocation strategy for user {}", userId);
        saveAllocationGoalsUseCase.execute(userId, goals);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AllocationGoalDto>> getAllocationGoals(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request to get allocation strategy for user {}", userId);
        List<AllocationGoalDto> goals = getAllocationGoalsUseCase.execute(userId);
        return ResponseEntity.ok(goals);
    }
}
