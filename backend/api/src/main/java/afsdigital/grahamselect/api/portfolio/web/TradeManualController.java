package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.portfolio.application.usecase.CreateManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeManualController {

    private final CreateManualTradeUseCase createManualTradeUseCase;

    @PostMapping("/manual")
    public ResponseEntity<Trade> createManualTrade(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ManualTradeRequest request
    ) {
        String userId = jwt.getSubject();

        Trade trade = Trade.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .ticker(request.ticker())
                .side(request.side())
                .tradeDate(request.tradeDate())
                .quantity(request.quantity())
                .price(request.price())
                .broker(request.broker())
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        Trade savedTrade = createManualTradeUseCase.execute(trade);

        if (savedTrade == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrade);
    }

    public record ManualTradeRequest(
            @NotBlank String ticker,
            @NotBlank String side,
            @NotNull LocalDate tradeDate,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal price,
            @NotBlank String broker
    ) {
    }
}
