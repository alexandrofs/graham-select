package afsdigital.grahamselect.common.dividends.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RelevantFactDTO(
        UUID id,
        String ticker,
        String title,
        String summary,
        String sourceUrl,
        String category,
        Instant publicationDate
) {
}
