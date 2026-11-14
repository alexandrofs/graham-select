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
public class AssetTag {
    private UUID id;
    private String userId;
    private UUID tagId;
    private String ticker;
    private LocalDateTime createdAt;
}
