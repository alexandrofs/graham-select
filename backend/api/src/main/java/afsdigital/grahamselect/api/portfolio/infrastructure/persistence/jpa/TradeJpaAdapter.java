package afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TradeJpaAdapter implements TradePort {

    private final JpaTradeRepository repository;

    @Override
    public Trade save(Trade trade) {
        String normalizedProfileId = normalizeBlankToNull(trade.getProfileId());
        String normalizedAssetClass = normalizeBlankToNull(trade.getAssetClass());
        TradeEntity entity = TradeEntity.builder()
                .id(trade.getId())
                .userId(trade.getUserId())
                .profileId(normalizedProfileId)
                .ticker(trade.getTicker())
                .side(trade.getSide())
                .tradeDate(trade.getTradeDate())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .broker(trade.getBroker())
                .assetClass(normalizedAssetClass)
                .createdAt(trade.getCreatedAt())
                .build();

        return toDomain(repository.save(entity));
    }

    @Override
    public boolean exists(
            String userId,
            String profileId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker,
            String side
    ) {
        if (profileId == null || profileId.isBlank()) {
            return repository.existsByUserIdAndProfileIdIsNullAndTickerAndTradeDateAndQuantityAndPriceAndBrokerAndSide(
                    userId, ticker, tradeDate, quantity, price, broker, side
            );
        }
        return repository.existsByUserIdAndProfileIdAndTickerAndTradeDateAndQuantityAndPriceAndBrokerAndSide(
                userId, profileId, ticker, tradeDate, quantity, price, broker, side
        );
    }

    @Override
    public boolean existsOtherWithSameSignature(
            UUID excludeId,
            String userId,
            String profileId,
            String ticker,
            LocalDate tradeDate,
            BigDecimal quantity,
            BigDecimal price,
            String broker,
            String side
    ) {
        if (profileId == null || profileId.isBlank()) {
            return repository.existsByIdNotAndUserIdAndProfileIdIsNullAndTickerAndTradeDateAndQuantityAndPriceAndBrokerAndSide(
                    excludeId, userId, ticker, tradeDate, quantity, price, broker, side
            );
        }
        return repository.existsByIdNotAndUserIdAndProfileIdAndTickerAndTradeDateAndQuantityAndPriceAndBrokerAndSide(
                excludeId, userId, profileId, ticker, tradeDate, quantity, price, broker, side
        );
    }

    @Override
    public Optional<Trade> findByIdAndUserId(UUID id, String userId) {
        return repository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public void deleteByIdAndUserId(UUID id, String userId) {
        repository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public List<Trade> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Trade toDomain(TradeEntity saved) {
        return Trade.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .profileId(saved.getProfileId())
                .ticker(saved.getTicker())
                .side(saved.getSide())
                .tradeDate(saved.getTradeDate())
                .quantity(saved.getQuantity())
                .price(saved.getPrice())
                .broker(saved.getBroker())
                .assetClass(saved.getAssetClass())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private String normalizeBlankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
