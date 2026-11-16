package afsdigital.grahamselect.common.dividends.application.usecase;

import afsdigital.grahamselect.common.dividends.application.dto.DividendScheduleDTO;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GetDividendScheduleUseCase {

    private final DividendAnnouncementPort dividendAnnouncementPort;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;

    public List<DividendScheduleDTO> execute(
            String userId,
            String specificTicker,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            boolean onlyMyTickers
    ) {
        log.info("Consultando agenda de proventos: userId={}, ticker={}, startDate={}, endDate={}, status={}, onlyMyTickers={}",
                userId, specificTicker, startDate, endDate, status, onlyMyTickers);

        Map<String, BigDecimal> userSharesMap = new HashMap<>();
        if (userId != null && !userId.isBlank() && getCustodyPositionsUseCase != null) {
            try {
                List<CustodyPositionDTO> positions = getCustodyPositionsUseCase.execute(userId);
                if (positions != null) {
                    for (CustodyPositionDTO pos : positions) {
                        if (pos.quantity() != null && pos.quantity().compareTo(BigDecimal.ZERO) > 0) {
                            userSharesMap.merge(pos.ticker().toUpperCase(), pos.quantity(), BigDecimal::add);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao obter posições de custódia para userId={}: {}", userId, e.getMessage(), e);
            }
        }

        List<String> tickersFilter = null;
        if (onlyMyTickers) {
            if (userSharesMap.isEmpty()) {
                return Collections.emptyList();
            }
            if (specificTicker != null && !specificTicker.isBlank()) {
                String clean = specificTicker.trim().toUpperCase();
                if (!userSharesMap.containsKey(clean)) {
                    return Collections.emptyList();
                }
                tickersFilter = List.of(clean);
            } else {
                tickersFilter = userSharesMap.keySet().stream().toList();
            }
        } else if (specificTicker != null && !specificTicker.isBlank()) {
            tickersFilter = List.of(specificTicker.trim().toUpperCase());
        }

        List<DividendAnnouncement> announcements = dividendAnnouncementPort.findByTickersAndDateRange(
                tickersFilter,
                startDate,
                endDate,
                status
        );

        return announcements.stream()
                .map(ann -> {
                    BigDecimal shares = userSharesMap.getOrDefault(ann.getTicker().toUpperCase(), BigDecimal.ZERO);
                    BigDecimal estimatedTotal = null;
                    BigDecimal estimatedNetTotal = null;

                    if (shares.compareTo(BigDecimal.ZERO) > 0) {
                        if (ann.getRate() != null) {
                            estimatedTotal = shares.multiply(ann.getRate()).setScale(2, RoundingMode.HALF_UP);
                        }
                        if (ann.getNetValue() != null) {
                            estimatedNetTotal = shares.multiply(ann.getNetValue()).setScale(2, RoundingMode.HALF_UP);
                        }
                    }

                    return new DividendScheduleDTO(
                            ann.getId(),
                            ann.getTicker(),
                            ann.getType(),
                            ann.getRate(),
                            ann.getNetValue(),
                            ann.getApprovedDate(),
                            ann.getComDate(),
                            ann.getExDate(),
                            ann.getPaymentDate(),
                            ann.getSource(),
                            ann.getStatus(),
                            shares.compareTo(BigDecimal.ZERO) > 0 ? shares : null,
                            estimatedTotal,
                            estimatedNetTotal
                    );
                })
                .sorted(Comparator.comparing(
                        (DividendScheduleDTO d) -> d.paymentDate() != null ? d.paymentDate() : d.comDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(DividendScheduleDTO::ticker))
                .toList();
    }
}
