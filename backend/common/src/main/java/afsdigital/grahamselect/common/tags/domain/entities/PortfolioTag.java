package afsdigital.grahamselect.common.tags.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTag {
    private UUID id;
    private String userId;
    private String name;
    private String normalizedName;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
