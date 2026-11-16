package afsdigital.grahamselect.common.dividends.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RelevantFact {

    private UUID id;
    private String ticker;
    private String title;
    private String summary;
    private String sourceUrl;
    private String category;
    private Instant publicationDate;
    private Instant createdAt;

    public static class RelevantFactBuilder {
        public RelevantFact build() {
            if (this.ticker == null || this.ticker.isBlank()) {
                throw new IllegalArgumentException("ticker cannot be null or empty");
            }
            if (this.title == null || this.title.isBlank()) {
                throw new IllegalArgumentException("title cannot be null or empty");
            }
            if (this.category == null || this.category.isBlank()) {
                this.category = "FATO_RELEVANTE";
            }
            if (this.id == null) {
                this.id = UUID.randomUUID();
            }
            if (this.publicationDate == null) {
                this.publicationDate = Instant.now();
            }
            if (this.createdAt == null) {
                this.createdAt = Instant.now();
            }
            return new RelevantFact(
                    this.id,
                    this.ticker.trim().toUpperCase(),
                    this.title.trim(),
                    this.summary != null ? this.summary.trim() : null,
                    this.sourceUrl != null ? this.sourceUrl.trim() : null,
                    this.category.trim().toUpperCase(),
                    this.publicationDate,
                    this.createdAt
            );
        }
    }
}
