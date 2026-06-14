package afsdigital.grahamselect.api.goals.web;

import afsdigital.grahamselect.goals.application.dto.CreateFinancialGoalRequest;
import afsdigital.grahamselect.goals.application.dto.FinancialGoalDto;
import afsdigital.grahamselect.goals.application.usecase.CreateFinancialGoalUseCase;
import afsdigital.grahamselect.goals.application.usecase.GetFinancialGoalUseCase;
import afsdigital.grahamselect.goals.application.usecase.UpdateFinancialGoalUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financial-goals")
@RequiredArgsConstructor
@Slf4j
public class FinancialGoalsController {

    private final CreateFinancialGoalUseCase createUseCase;
    private final UpdateFinancialGoalUseCase updateUseCase;
    private final GetFinancialGoalUseCase getUseCase;

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateFinancialGoalRequest request) {
        String userId = jwt.getSubject();
        log.info("Received request to create financial goal for user {}", userId);
        FinancialGoalDto result = createUseCase.execute(userId, request);
        return ResponseEntity.status(201).body(Map.of("data", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreateFinancialGoalRequest request) {
        String userId = jwt.getSubject();
        log.info("Received request to update financial goal for user {}", userId);
        FinancialGoalDto result = updateUseCase.execute(id, userId, request);
        return ResponseEntity.ok(Map.of("data", result));
    }

    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request to get financial goal for user {}", userId);
        Optional<FinancialGoalDto> result = getUseCase.execute(userId);
        return ResponseEntity.ok(Map.of("data", result.map(java.util.List::of).orElse(java.util.List.of())));
    }
}
