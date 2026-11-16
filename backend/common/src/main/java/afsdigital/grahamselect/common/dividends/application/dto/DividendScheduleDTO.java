package afsdigital.grahamselect.common.dividends.application.dto;

import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DividendScheduleDTO(
        UUID id,
        String ticker,
        CorporateEventType type,
        BigDecimal rate,
        BigDecimal netValue,
        LocalDate approvedDate,
        LocalDate comDate,
        LocalDate exDate,
        LocalDate paymentDate,
        String source,
        String status,
        BigDecimal userShares,
        BigDecimal estimatedTotal,
        BigDecimal estimatedNetTotal
) {
}
