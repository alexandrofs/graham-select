package afsdigital.grahamselect.common.dividends.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DividendAnnouncement {

    private UUID id;
    private String ticker;
    private CorporateEventType type;
    private BigDecimal rate;
    private BigDecimal netValue;
    private LocalDate approvedDate;
    private LocalDate comDate;
    private LocalDate exDate;
    private LocalDate paymentDate;
    private String source;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public static class DividendAnnouncementBuilder {
        public DividendAnnouncement build() {
            if (this.ticker == null || this.ticker.isBlank()) {
                throw new IllegalArgumentException("ticker cannot be null or empty");
            }
            if (this.type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }
            if (this.rate == null) {
                throw new IllegalArgumentException("rate cannot be null");
            }
            if (this.comDate == null) {
                throw new IllegalArgumentException("comDate cannot be null");
            }
            if (this.source == null || this.source.isBlank()) {
                throw new IllegalArgumentException("source cannot be null or empty");
            }
            if (this.status == null || this.status.isBlank()) {
                this.status = "APPROVED";
            }
            if (this.id == null) {
                this.id = UUID.randomUUID();
            }
            if (this.createdAt == null) {
                this.createdAt = Instant.now();
            }
            if (this.updatedAt == null) {
                this.updatedAt = Instant.now();
            }
            if (this.netValue == null) {
                this.netValue = calculateNetValue(this.rate, this.type);
            }
            return new DividendAnnouncement(
                    this.id,
                    this.ticker.trim().toUpperCase(),
                    this.type,
                    this.rate.setScale(6, RoundingMode.HALF_UP),
                    this.netValue.setScale(6, RoundingMode.HALF_UP),
                    this.approvedDate,
                    this.comDate,
                    this.exDate,
                    this.paymentDate,
                    this.source.trim().toUpperCase(),
                    this.status.trim().toUpperCase(),
                    this.createdAt,
                    this.updatedAt
            );
        }

        private BigDecimal calculateNetValue(BigDecimal rate, CorporateEventType type) {
            if (type == CorporateEventType.JCP) {
                return rate.multiply(BigDecimal.valueOf(0.85)).setScale(6, RoundingMode.HALF_UP);
            }
            return rate.setScale(6, RoundingMode.HALF_UP);
        }
    }
}
