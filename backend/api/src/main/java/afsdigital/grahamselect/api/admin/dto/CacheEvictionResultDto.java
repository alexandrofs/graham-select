package afsdigital.grahamselect.api.admin.dto;

import java.time.OffsetDateTime;

public record CacheEvictionResultDto(
        String message,
        OffsetDateTime timestamp
) {
}
