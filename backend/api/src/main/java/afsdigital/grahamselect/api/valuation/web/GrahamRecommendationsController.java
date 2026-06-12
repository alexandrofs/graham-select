package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequirePremium;
import afsdigital.grahamselect.api.valuation.infrastructure.kafka.ValuationRequestedPublisher;
import afsdigital.grahamselect.common.domain.entities.ValuationRequestedEvent;
import afsdigital.grahamselect.valuation.application.dto.GrahamRecommendationDto;
import afsdigital.grahamselect.valuation.application.usecase.GetGrahamRecommendationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/graham-recommendations")
@RequiredArgsConstructor
@Slf4j
@RequirePremium
public class GrahamRecommendationsController {

    private final GetGrahamRecommendationsUseCase getGrahamRecommendationsUseCase;
    private final ValuationRequestedPublisher valuationRequestedPublisher;
    private final java.util.concurrent.ConcurrentHashMap<String, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    @GetMapping
    public ResponseEntity<List<GrahamRecommendationDto>> getRecommendations(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request to get Graham recommendations for user {}", userId);

        List<GrahamRecommendationDto> recommendations = getGrahamRecommendationsUseCase.execute(userId);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/trigger")
    public ResponseEntity<Void> triggerCalculation(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request to trigger Graham recommendations for user {}", userId);

        long now = System.currentTimeMillis();
        Long lastTrigger = cooldowns.get(userId);
        if (lastTrigger != null && (now - lastTrigger) < 10000) {
            log.warn("Rate limit triggered for user {} on graham recommendations calculation", userId);
            return ResponseEntity.status(429).build();
        }
        cooldowns.put(userId, now);

        ValuationRequestedEvent event = new ValuationRequestedEvent(
                userId,
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                UUID.randomUUID()
        );

        valuationRequestedPublisher.publish(event);

        return ResponseEntity.accepted().build();
    }
}
