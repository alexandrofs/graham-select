package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.portfolio.application.usecase.CreateManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.DeleteManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.ListManualTradesUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.UpdateManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
@Slf4j
public class TradeManualController {

    private final CreateManualTradeUseCase createManualTradeUseCase;
    private final ListManualTradesUseCase listManualTradesUseCase;
    private final UpdateManualTradeUseCase updateManualTradeUseCase;
    private final DeleteManualTradeUseCase deleteManualTradeUseCase;
    private final FamilyProfilePort familyProfilePort;

    @GetMapping("/manual")
    public ResponseEntity<List<Trade>> listManualTrades(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("REST API Request received to list manual trades for user: {}", userId);
        return ResponseEntity.ok(listManualTradesUseCase.execute(userId));
    }

    @PostMapping("/manual")
    public ResponseEntity<Trade> createManualTrade(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ManualTradeRequest request
    ) {
        String userId = jwt.getSubject();
        log.info("REST API Request received to create manual trade for user: {}, ticker: {}", userId, request.ticker());

        Trade trade = buildTradeFromRequest(UUID.randomUUID(), userId, request);
        Trade savedTrade = createManualTradeUseCase.execute(trade);

        if (savedTrade == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrade);
    }

    @PutMapping("/manual/{id}")
    public ResponseEntity<Trade> updateManualTrade(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ManualTradeRequest request
    ) {
        String userId = jwt.getSubject();
        log.info("REST API Request received to update manual trade: id={}, user={}, ticker={}", id, userId, request.ticker());

        Trade updatedFields = buildTradeFromRequest(id, userId, request);
        UpdateManualTradeUseCase.UpdateResult result = updateManualTradeUseCase.execute(id, userId, updatedFields);

        return switch (result.status()) {
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case UPDATED -> ResponseEntity.ok(result.trade());
        };
    }

    @DeleteMapping("/manual/{id}")
    public ResponseEntity<Void> deleteManualTrade(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        String userId = jwt.getSubject();
        log.info("REST API Request received to delete manual trade: id={}, user={}", id, userId);

        boolean deleted = deleteManualTradeUseCase.execute(id, userId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private Trade buildTradeFromRequest(UUID id, String userId, ManualTradeRequest request) {
        String normalizedProfileId = resolveAndValidateProfileId(request.profileId(), userId);
        String normalizedSide = normalizeSide(request.side());
        String normalizedBroker = (request.broker() == null || request.broker().isBlank())
                ? "Manual"
                : request.broker().trim();
        String normalizedAssetClass = (request.assetClass() == null || request.assetClass().isBlank())
                ? null
                : request.assetClass().trim();

        return Trade.builder()
                .id(id)
                .userId(userId)
                .profileId(normalizedProfileId)
                .ticker(request.ticker().trim().toUpperCase(Locale.ROOT))
                .side(normalizedSide)
                .tradeDate(request.tradeDate())
                .quantity(request.quantity())
                .price(request.price())
                .broker(normalizedBroker)
                .assetClass(normalizedAssetClass)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private String normalizeSide(String side) {
        if (side == null || side.isBlank()) {
            return "COMPRA";
        }
        String normalized = side.trim().toUpperCase(Locale.ROOT);
        if (!"COMPRA".equals(normalized) && !"VENDA".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de operação deve ser COMPRA ou VENDA.");
        }
        return normalized;
    }

    private String resolveAndValidateProfileId(String profileId, String userId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        UUID parsedId;
        try {
            parsedId = UUID.fromString(profileId.trim());
        } catch (IllegalArgumentException e) {
            log.error("Invalid profileId UUID format in manual trade: userId={}, profileId={}", userId, profileId, e);
            throw new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
        }
        familyProfilePort.findActiveByIdAndMasterUserId(parsedId, userId)
                .orElseThrow(() -> {
                    log.error("Family profile not found or does not belong to user: userId={}, profileId={}", userId, parsedId);
                    return new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
                });
        return parsedId.toString();
    }

    public record ManualTradeRequest(
            @NotBlank @Size(max = 20) String ticker,
            String side,
            @NotNull LocalDate tradeDate,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal price,
            @Size(max = 100) String broker,
            @Size(max = 50) String assetClass,
            String profileId
    ) {
    }
}
