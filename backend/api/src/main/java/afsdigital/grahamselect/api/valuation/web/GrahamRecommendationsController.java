package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequirePremium;
import afsdigital.grahamselect.api.valuation.infrastructure.kafka.ValuationRequestedPublisher;
import afsdigital.grahamselect.common.domain.entities.ValuationRequestedEvent;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.valuation.application.dto.GrahamRecommendationDto;
import afsdigital.grahamselect.valuation.application.usecase.GetGrahamRecommendationsUseCase;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final PortfolioTagPort portfolioTagPort;
    private final java.util.concurrent.ConcurrentHashMap<String, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    @GetMapping
    public ResponseEntity<List<GrahamRecommendationDto>> getRecommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId
    ) {
        String userId = jwt.getSubject();
        log.info("Received request to get Graham recommendations for user {}, tagId={}", sanitize(userId), tagId);

        List<GrahamRecommendationDto> recommendations = tagId == null
                ? getGrahamRecommendationsUseCase.execute(userId)
                : getGrahamRecommendationsUseCase.execute(userId, tagId);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/trigger")
    public ResponseEntity<Void> triggerCalculation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID tagId
    ) {
        String userId = jwt.getSubject();
        log.info("Received request to trigger Graham recommendations for user {}, tagId={}", sanitize(userId), tagId);

        if (tagId != null && portfolioTagPort.findByIdAndUserId(tagId, userId).isEmpty()) {
            TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
            log.error("Tag não encontrada ou acesso negado ao disparar recomendações de Graham. userId={}, tagId={}",
                    sanitize(userId), tagId, ex);
            throw ex;
        }

        String scopeKey = AllocationScope.fromTagId(tagId);
        String cooldownKey = userId + ":" + scopeKey;

        long now = System.currentTimeMillis();
        Long lastTrigger = cooldowns.get(cooldownKey);
        if (lastTrigger != null && (now - lastTrigger) < 10000) {
            log.warn("Rate limit triggered for user {} on graham recommendations calculation, scopeKey={}",
                    sanitize(userId), scopeKey);
            return ResponseEntity.status(429).build();
        }
        cooldowns.put(cooldownKey, now);

        ValuationRequestedEvent event = new ValuationRequestedEvent(
                userId,
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                UUID.randomUUID(),
                tagId
        );

        valuationRequestedPublisher.publish(event);

        return ResponseEntity.accepted().build();
    }

    private static String sanitize(String value) {
        return value == null ? "null" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
